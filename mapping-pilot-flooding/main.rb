#!/usr/bin/ruby

require 'httparty'
require 'sparql/client'
require 'rdf/turtle'
require 'rdf/rdfxml'
require 'json'
require 'sinatra'
require 'pry-byebug'
require 'securerandom'

Encoding.default_external = Encoding::UTF_8
Encoding.default_internal = Encoding::UTF_8

class String
  def to_a
    lines.to_a
  end
end

# resp = HTTParty.post("http://localhost/foo",
#                      :basic_auth => auth,
#                      :headers => { 'Content-Type' => 'application/json' },
#                      :body => {:docs => rows}.to_json)

# wget http://localhost/api/taxonomy/ad20fc17-78fa-43ca-9d5c-32129fe1c823/mappings?_=1454055952533 -O $(date +"%Y-%m-%d-mappings.rdf")


configure do
  set :bind, '0.0.0.0'
  set :graph, ENV['MU_APPLICATION_GRAPH']
  set :floodTimeout, (ENV['FLOOD_TIMEOUT'] or 60)
  set :minContext, (ENV['MIN_CONTEXT'] or 0.35)
  set :sparql_client, SPARQL::Client.new('http://database:8890/sparql', { :read_timeout => 1000000 } )
end


###
# Vocabularies
###

include RDF
MU = RDF::Vocabulary.new('http://mu.semte.ch/vocabularies/')
MT = RDF::Vocabulary.new('http://sem.tenforce.com/vocabularies/mapping-pilot/')

should_flood = false
is_flooding = false
flood_mutex = Mutex.new

get '/taxonomy/:taxonomy/mappings' do
  if request.accept? 'application/rdf+xml'
    content_type 'application/rdf+xml'
    get_mappings_rdf(params[:taxonomy])
  else
    content_type 'text/csv'
    get_mappings_csv(params[:taxonomy])
  end
end

get '/taxonomy/:taxonomy/distribution' do
  full_score = params[:score_pred]
  score_distribution(params[:taxonomy], full_score).to_json
end

get '/taxonomy/:taxonomy/matchCounts' do
  from = match_counts(params[:taxonomy])
  to = match_counts_to(params[:taxonomy])

  all = {}
  from.map do |item|
    all[item[:uri]] = { uri: item[:uri], id: item[:id], name: item[:name], from: { matches: item[:matches], openmatches: item[:openmatches] } }
  end
  to.map do |item|
    existing = all[item[:uri]]
    if not existing
      existing = {}
      all[item[:uri]] = existing
    end
    existing[:to] = { matches: item[:matches], openmatches: item[:openmatches] }
  end

  result = all.map do |key, value|
    value
  end
  result.to_json
end

post '/setup' do
  add_uuids()
  cleanup_mappings()
  normalize_scores()
  build_flooders()
  flood_all()
  "ok"
end

post '/flood_scores' do
  flood_mutex.synchronize do
    if not should_flood
      should_flood = true
      if not is_flooding
        Thread.new do
          while should_flood do
            sleep(settings.floodTimeout)
            flood_mutex.synchronize do
              should_flood = false
              is_flooding = true
            end
            flood_all()
            flood_mutex.synchronize do
              is_flooding = false
            end
          end
        end
      end
    end
  end
  "ok"
end

get '/flood_status' do
  if is_flooding
    'FLOODING'
  elsif should_flood
    'WAITING'
  else
    'DONE'
  end
end

post '/load_stopwords' do
  load_all_stopwords()
  "ok"
end

post '/load' do
  load_data()
  "ok"
end

post '/taxonomy/:taxonomy/stopwords' do
  request.body.rewind
  taxonomy = params[:taxonomy]
  data = JSON.parse(request.body.read)
  save_stopwords taxonomy, data['stopwords']
end

get '/taxonomy/:taxonomy/statistics' do
  concepts = nil
  matches = nil
  nomatches = nil
  nosuggestions = nil
  t1 = Thread.new do
    concepts = get_concept_counts(params[:taxonomy])
  end
  t2 = Thread.new do
    matches = get_match_counts(params[:taxonomy])
  end
  t3 = Thread.new do
    nomatches = get_nomatch_counts(params[:taxonomy])
  end
  t4 = Thread.new do
    nosuggestions = get_nosuggestion_counts(params[:taxonomy])
  end
  t1.join
  t2.join
  t3.join
  t4.join
  { concepts: concepts, matches: matches, nomatches: nomatches, nosuggestions: nosuggestions }.to_json
end

get '/taxonomy/:taxonomy/stopwords' do
  taxonomy = params[:taxonomy]
  hidden = (params[:hidden] == "true")
  list_stopwords(taxonomy, hidden).to_json
end

###
# Helpers
###

helpers do

  # adds uuids to all concepts that don't have uuids yet
  def add_uuids
    query = "PREFIX mt: <#{MT}>
PREFIX mu: <#{MU}>
INSERT {
  GRAPH <#{settings.graph}> {
    ?s mu:uuid ?uuid
  }
}WHERE {
  GRAPH <#{settings.graph}> {
    ?s a ?thing.
    { ?s a mt:Mapping. } UNION { ?s a skos:Concept. }
    OPTIONAL {
      ?s mu:uuid ?current.
    }
    BIND(IF(BOUND(?current), ?current, STRUUID()) AS ?uuid)
  }
}"
    sparqlUpdate(query)
    "ok"
  end

  def match_counts (taxonomy)
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
SELECT ?uri ?id ?name COUNT(DISTINCT(?match)) as ?matches COUNT(DISTINCT(?match2)) as ?openmatches
WHERE {
  ?uri a skos:Concept .
  ?uri skos:inScheme ?taxonomy .
  ?uri mu:uuid ?id .
  ?taxonomy mu:uuid \"#{taxonomy}\" .
  FILTER NOT EXISTS {
    ?other skos:broader ?uri.
  }
  ?uri skos:prefLabel ?name .
  OPTIONAL {
    ?mapping a mt:Mapping .
    ?mapping mt:matchType ?type .
    ?mapping mt:mapsFrom ?uri .
    ?mapping mt:mapsTo ?match .
    FILTER (?type in ('yes', 'broad', 'narrow', 'close'))
  }
  OPTIONAL {
    ?mapping2 a mt:Mapping .
    OPTIONAL {
      ?mapping2 mt:matchType ?type2 .
    }
    FILTER(?type2 = \"todo\" || !(bound(?type2)))
    ?mapping2 mt:mapsFrom ?uri .
    ?mapping2 mt:mapsTo ?match2 .
  }
} group by ?uri ?id ?name order by DESC(?matches) ?name"
    solutions_to_hash_list(sparqlQuery(query))
  end

  def match_counts_to (taxonomy)
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
SELECT ?uri ?name ?id COUNT(DISTINCT(?match)) as ?matches COUNT(DISTINCT(?match2)) as ?openmatches
WHERE {
  ?uri a skos:Concept .
  ?uri skos:inScheme ?taxonomy .
  ?uri mu:uuid ?id .
  ?taxonomy mu:uuid \"#{taxonomy}\" .
  FILTER NOT EXISTS {
    ?other skos:broader ?uri.
  }
  ?uri skos:prefLabel ?name .
  OPTIONAL {
    ?mapping a mt:Mapping .
    ?mapping mt:matchType ?type .
    ?mapping mt:mapsFrom ?match .
    ?mapping mt:mapsTo ?uri .
    FILTER (?type in ('yes', 'broad', 'narrow', 'close'))
  }
  OPTIONAL {
    ?mapping2 a mt:Mapping .
    OPTIONAL {
      ?mapping2 mt:matchType ?type2 .
    }
    FILTER(?type2 = \"todo\" || !(bound(?type2)))
    ?mapping2 mt:mapsFrom ?match2 .
    ?mapping2 mt:mapsTo ?uri .
  }
} group by ?uri ?id ?name order by DESC(?matches) ?name"
    solutions_to_hash_list(sparqlQuery(query))
  end

  def flood_all
    combine_scores(true)
    boost_scores(true)
    flooders = get_flooders()
    flooders.each do |flooder|
      clean_flooder(flooder)
    end

    flooders.each do |flooder|
      flood_scores(flooder)
    end
    combine_scores(false)
    boost_scores(false)
    normalize_scores("finish")
    backup_scores()
    update_manual_matches()
  end

  def update_manual_matches
    cleanup = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{settings.graph}> {
    ?s mt:combinedScore ?score.
  }
} where {
  GRAPH <#{settings.graph}> {
    ?s a mt:Mapping.
    ?s mt:matchType ?type.
    ?s mt:combinedScore ?score.
   }
}"
    sparqlUpdate(cleanup)
    query = "PREFIX mt: <#{MT}>
INSERT {
  GRAPH <#{settings.graph}> {
    ?s mt:combinedScore ?newScore.
  }
} where {
  GRAPH <#{settings.graph}> {
    ?s a mt:Mapping.
    ?s mt:matchType ?type.
    ?s mt:originalScore ?score.
    BIND ( IF(?type in (\"yes\", \"broad\", \"narrow\"), 1, IF(?type = \"no\", 0, ?score)) AS ?newScore)
  }
}"
    sparqlUpdate(query)
  end

  def get_concept_counts (taxonomy)
    # note the innermost filter, wtf virtuoso, really...
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
SELECT  COUNT( DISTINCT ?origin) as ?fromCount COUNT (DISTINCT ?otherConcepts) as ?toCount  WHERE {
  GRAPH <#{settings.graph}> {
    ?taxonomy mu:uuid \"#{taxonomy}\" .
    {SELECT ?taxonomy SAMPLE(?targetScheme) as ?targetScheme WHERE {
      ?mapping a mt:Mapping .
      ?from skos:inScheme ?taxonomy.
      ?mapping mt:mapsFrom ?from .
      ?mapping mt:mapsTo ?concept .
      ?concept skos:inScheme ?targetScheme .
      FILTER (?targetScheme != ?taxonomy )
    } GROUP BY ?taxonomy }
    ?origin skos:inScheme ?taxonomy.
    FILTER NOT EXISTS {
      ?other skos:broader ?origin.
    }
    ?otherConcepts skos:inScheme ?targetScheme .
    FILTER NOT EXISTS {
      ?other2 skos:broader ?otherConcepts.
    }
  }
}"
    solutions_to_hash_list(sparqlQuery(query))[0]
  end

  def get_nomatch_counts (taxonomy)
    query = "PREFIX mt: <http://sem.tenforce.com/vocabularies/mapping-pilot/>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
   SELECT COUNT(DISTINCT ?nomatch) as ?countnomatch WHERE {
     GRAPH <#{settings.graph}> {
      ?nomatch a skos:Concept .
      FILTER NOT EXISTS {
        ?thing skos:broader ?nomatch.
      }
      ?nomatch skos:inScheme ?taxonomy.
      ?taxonomy mu:uuid \"#{taxonomy}\" .
      FILTER NOT EXISTS {
        ?mapping a mt:Mapping .
        ?mapping mt:mapsFrom ?nomatch .
        ?mapping mt:matchType ?type.
        FILTER(?type in (\"yes\", \"broad\", \"narrow\", \"close\"))
      }
    }}"
    solutions_to_hash_list(sparqlQuery(query))[0][:countnomatch]
  end

  def get_nosuggestion_counts (taxonomy)
    query = "PREFIX mt: <http://sem.tenforce.com/vocabularies/mapping-pilot/>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
   SELECT COUNT(DISTINCT ?nosuggestion) as ?countnosuggestion WHERE {
     GRAPH <#{settings.graph}> {
      ?nosuggestion a skos:Concept .
      ?nosuggestion skos:inScheme ?taxonomy.
      ?taxonomy mu:uuid \"#{taxonomy}\" .
      FILTER NOT EXISTS {
        ?thing skos:broader ?nosuggestion.
      }
      FILTER NOT EXISTS {
        ?mapping2 a mt:Mapping .
        OPTIONAL {
          ?mapping2 mt:matchType ?type .
          FILTER(?type != \"no\")
        }
        ?mapping2 mt:mapsFrom ?nosuggestion .
      }
    }}"
    solutions_to_hash_list(sparqlQuery(query))[0][:countnosuggestion]
  end

  def get_match_counts (taxonomy)
    query = "PREFIX mt: <http://sem.tenforce.com/vocabularies/mapping-pilot/>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
   SELECT ?kind COUNT(DISTINCT ?mapping) as ?count WHERE {
     GRAPH <#{settings.graph}> {
      ?taxonomy mu:uuid \"#{taxonomy}\" .
       ?mapping a mt:Mapping .
      ?mapping mt:mapsFrom ?from .
      ?from skos:inScheme ?taxonomy.
      OPTIONAL {
        ?mapping mt:matchType ?type.
      }
      BIND (IF(?type = \"no\", \"no\", IF(BOUND(?type) && ?type != \"todo\", \"yes\", \"todo\")) AS ?kind)
    }} GROUP BY ?kind"
    solutions_to_hash_list(sparqlQuery(query))
  end

  def get_mappings_csv (taxonomy)
    graph = settings.graph
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
SELECT DISTINCT ?from ?fromLabel ?to ?toLabel ?predicate WHERE {
               GRAPH <#{graph}> {
                 ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> mt:Mapping .
                 ?s mt:mapsFrom ?from.
                 ?s mt:mapsTo ?to.
                 ?s mt:matchType ?type.
                 ?from skos:inScheme ?tax .
                 ?from skos:prefLabel ?fromLabel.
                 ?to skos:prefLabel ?toLabel.
                 FILTER(lang(?fromLabel) = \"en\")
                 FILTER(lang(?toLabel) = \"en\")
                 ?tax mu:uuid \"#{taxonomy}\" .
                 FILTER (?type in (\"yes\", \"broad\", \"narrow\", \"close\"))
                 BIND( IF (?type = \"yes\", skos:exactMatch, IF (?type = \"close\", skos:closeMatch, IF(?type = \"broad\", skos:broadMatch, IF(?type = \"narrow\", skos:narrowMatch, skos:related)))) AS ?predicate)
               }
             }"
    result = solutions_to_hash_list(sparqlQuery(query))
    csv = "from|fromLabel|to|toLabel|match type\n"
    result.map do |item|
      row = "#{item[:from]}|#{item[:fromLabel]}|#{item[:to]}|#{item[:toLabel]}|#{item[:predicate]}\n"
      csv += row
    end

    csv
  end

  def get_mappings_rdf (taxonomy)
    graph = settings.graph
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
CONSTRUCT {
  ?from ?predicate ?to .
  ?from skos:prefLabel ?fromLabel.
  ?to skos:prefLabel ?toLabel.
  ?from <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> skos:Concept .
  ?to <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> skos:Concept .
} WHERE {
               GRAPH <#{graph}> {
                 FILTER (?type in (\"yes\", \"broad\", \"narrow\", \"close\"))
                 ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> mt:Mapping .
                 ?s mt:mapsFrom ?from.
                 ?s mt:mapsTo ?to.
                 ?s mt:matchType ?type.
                 ?from skos:inScheme ?tax .
                 ?from skos:prefLabel ?fromLabel.
                 ?to skos:prefLabel ?toLabel.
                 FILTER(lang(?fromLabel) = \"en\")
                 FILTER(lang(?toLabel) = \"en\")
                 ?tax mu:uuid \"#{taxonomy}\" .
                 FILTER (?type in (\"yes\", \"broad\", \"narrow\", \"close\"))
                 BIND( IF (?type = \"yes\", skos:exactMatch, IF (?type = \"close\", skos:closeMatch, IF(?type = \"broad\", skos:broadMatch, IF(?type = \"narrow\", skos:narrowMatch, skos:related)))) AS ?predicate)
               } 
             }"
    result = sparqlConstruct query
    rdfxml = RDF::RDFXML::Writer.buffer do |writer|
      result.each_statement do |statement|
        writer << statement
      end
    end
    rdfxml
  end

  def save_stopwords (taxonomy, stopwords)
    #delete previous
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
DELETE {
  GRAPH <#{settings.graph}> {
    ?settings mt:stopword ?stopword.
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?cs a skos:ConceptScheme .
    ?cs mu:uuid \"#{taxonomy}\" .
    ?settings a mt:MappingSettings .
    ?settings mt:settingsFor ?cs .
    ?settings mt:stopword ?stopword .
  }
}"

    sparqlUpdate(query)

    hiddenStopwords = list_hidden_stopwords(taxonomy)
    
    stopwordList = ""
    stopwords.each do |stopword|
      if not hiddenStopwords.include?(stopword)
        stopwordList += "    ?settings mt:stopword \"#{stopword}\" .\n"
      end
    end

    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
INSERT {
  GRAPH <#{settings.graph}> {
#{stopwordList}
    ?settings a mt:MappingSettings .
    ?settings mt:settingsFor ?cs .
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?cs a skos:ConceptScheme .
    ?cs mu:uuid \"#{taxonomy}\" .
    OPTIONAL {
      ?existingSettings a mt:MappingSettings .
      ?existingSettings mt:settingsFor ?cs .
    }
    BIND( IF(BOUND(?existingSettings), ?existingSettings, <#{MT}/mappingSettings/#{SecureRandom.uuid}> ) AS ?settings)
  }
}"
    sparqlUpdate(query)
  end

  def list_stopwords (taxonomy, hidden)
    stopwords = "    ?settings mt:stopword ?stopword ."
    if hidden
      stopwords = " { {   ?settings mt:stopword ?stopword . } 
UNION { ?settings mt:hiddenStopword ?stopword . } }"
    end

    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
SELECT DISTINCT ?stopword WHERE {
  GRAPH <#{settings.graph}> {
    ?cs a skos:ConceptScheme .
    ?cs mu:uuid \"#{taxonomy}\" .
    ?settings a mt:MappingSettings .
    ?settings mt:settingsFor ?cs .
    #{stopwords}
  }
}"
    result = solutions_to_list(sparqlQuery(query))
  end

  def list_hidden_stopwords (taxonomy)
    stopwords = "    ?settings mt:hiddenStopword ?stopword ."

    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
SELECT DISTINCT ?stopword WHERE {
  GRAPH <#{settings.graph}> {
    ?cs a skos:ConceptScheme .
    ?cs mu:uuid \"#{taxonomy}\" .
    ?settings a mt:MappingSettings .
    ?settings mt:settingsFor ?cs .
    #{stopwords}
  }
}"
    result = solutions_to_list(sparqlQuery(query))
  end

  def combine_scores (beforeFlooding)
    combiners = get_combiners(beforeFlooding)
    combiners.map do |combinerName, combiner|
      run_combiner(combiner)
    end
  end

  def get_combiners (beforeFlooding)
    query = "PREFIX mt: <#{MT}>
SELECT DISTINCT ?combiner ?combinePred ?fromPred ?fromCoef ?normalizer
WHERE {
  ?combiner a mt:Combiner;
            mt:beforeFlooding #{beforeFlooding} ;
            mt:combinePart ?thing ;
            mt:combinePred ?combinePred .
  OPTIONAL {
    ?combiner mt:combineNormalizer ?normalizer.
  }
  ?thing mt:fromPred ?fromPred ;
         mt:fromCoef ?fromCoef .
}"
    resultSet = solutions_to_hash_list(sparqlQuery(query))
    result = {}
    resultSet.map do |row|
      combiner = result[row[:combiner]]
      if not combiner
        combiner = { parts: [], combinePred: row[:combinePred], normalizer: row[:normalizer] }
        result[row[:combiner]] = combiner
      end
      coef = row[:fromCoef].to_f
      combiner[:parts].push( { pred: row[:fromPred], coef: coef } )
      combiner[:normalizer] ||= 1
    end

    result
  end

  # boosters: [alpha*x + beta*y + gamma*z]/normalizer, capped at 1
  def run_combiner (combiner)
    combinePred = combiner[:combinePred]
    normalizer = combiner[:normalizer]

    coefficients = ""
    parts = ""
    combiner[:parts].each_with_index do |part,index|
      parts += "OPTIONAL { ?target <#{part[:pred]}> ?t#{index} . }
BIND (IF (BOUND(?t#{index}), ?t#{index}, 0) AS ?p#{index}) .\n"
      if index > 0
        coefficients += " + "
      end
      coefficients += "(xsd:float(#{part[:coef]})*?p#{index})"
    end

    query = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{settings.graph}> {
    ?target <#{combinePred}> ?oldscore .
  }
}INSERT {
  GRAPH <#{settings.graph}> {
    ?target <#{combinePred}> ?newscore .
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?target a mt:Mapping.
    OPTIONAL { ?target <#{combinePred}> ?oldscore. }
    {
      SELECT ?target (xsd:float(#{coefficients})/xsd:float(#{normalizer})) AS ?newscoreTemp WHERE {
        ?target a mt:Mapping.
        #{parts}
      }
    }
    BIND(IF(?newscoreTemp > 1, 1, ?newscoreTemp) AS ?newscore)
  }
}"
    sparqlUpdate(query)
  end

  def boost_scores (beforeFlooding)
    boosters = get_boosters(beforeFlooding)
    boosters.map do |boosterName, booster|
      run_booster(booster)
    end
  end

  def get_boosters (beforeFlooding)
    query = "PREFIX mt: <#{MT}>
SELECT DISTINCT ?booster ?boostPred ?boosted ?fromPred ?fromCoef ?boostTarget ?min ?maxTotal
WHERE {
  ?booster a mt:Booster;
            mt:beforeFlooding #{beforeFlooding} ;
            mt:boostPart ?thing ;
            mt:boosted ?boosted ;
            mt:boostPred ?boostPred .
  ?thing mt:boostOrigin ?fromPred ;
         mt:boostTarget ?boostTarget ;
         mt:boostCoef ?fromCoef .
  OPTIONAL {
    ?thing mt:boostMin ?min .
  }
  OPTIONAL {
    ?booster mt:boostMaxTotal ?maxTotal .
  }
}"
    resultSet = solutions_to_hash_list(sparqlQuery(query))
    result = {}
    resultSet.map do |row|
      booster = result[row[:booster]]
      if not booster
        booster = { parts: [], boosted: row[:boosted], boostPred: row[:boostPred] }
        max = 1
        if not row[:maxTotal].nil?
          max = row[:maxTotal]
        end
        booster[:maxTotal] = max
        result[row[:booster]] = booster
      end
      coef = row[:fromCoef].to_f
      target = row[:boostTarget].to_f
      min = -(1 - target)
      if not row[:min].nil?
        min = row[:min]
      end
      booster[:parts].push( { pred: row[:fromPred], coef: coef, target: target, min: min} )
    end

    result
  end

  # boosters: [base + coef*(max(min(1,score-target),MIN))+...]/maxTotal, capped at 1
  def run_booster (booster)
    boosterPred = booster[:boostPred]
    boosted = booster[:boosted]
    
    coefficients = ""
    max = booster[:maxTotal]
    parts = ""
    booster[:parts].each_with_index do |part,index|
      target = part[:target]
      parts += "OPTIONAL { ?target <#{part[:pred]}> ?t#{index} . }
BIND (IF (BOUND(?t#{index}), xsd:float(?t#{index}) - #{target}, 0) AS ?p#{index}) .\n"
      if index > 0
        coefficients += " + "
      end
      coef = part[:coef]
      min = part[:min]
      coefficients += "(xsd:float(#{coef})*(IF(?p#{index} < xsd:float(#{min}), xsd:float(#{min}), IF( ?p#{index} > 1, 1, ?p#{index}))))"
    end

    query = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{settings.graph}> {
    ?target <#{boosterPred}> ?oldscore .
  }
}INSERT {
  GRAPH <#{settings.graph}> {
    ?target <#{boosterPred}> ?newscore .
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?target a mt:Mapping.
    OPTIONAL { ?target <#{boosterPred}> ?oldscore. }
    {
      SELECT ?target IF(?tempNew < 0, 0, IF(?tempNew/xsd:float(#{max}) > 1, 1, ?tempNew/xsd:float(#{max}))) AS ?newscore WHERE {
        SELECT ?target xsd:float(?base + (#{coefficients})) AS ?tempNew WHERE {
          ?target a mt:Mapping.
          OPTIONAL {
            ?target <#{boosted}> ?b .
          }
          BIND(IF(BOUND(?b), ?b, 0) as ?base)
          #{parts}
        }
      }
    }
  }
}"
    sparqlUpdate(query)
  end  

  def backup_scores
query = "PREFIX mt: <#{MT}>
INSERT {
  GRAPH <#{settings.graph}> {
    ?target mt:originalScore ?score .
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?target a mt:Mapping.
    ?target mt:combinedScore ?score .
    FILTER NOT EXISTS {
      ?target mt:originalScore ?otherScore.
    }
  }
}"   
    sparqlUpdate(query)
  end

  def score_distribution (taxonomy, predicate)
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
SELECT COUNT(?mapping) as ?count STR(?start) as ?start STR(?end) as ?end WHERE {
{
  GRAPH <#{settings.graph}> {
    ?mapping a mt:Mapping .
    ?mapping <#{predicate}> ?score .
    ?mapping mt:mapsFrom ?concept.
    ?concept skos:inScheme ?cs .
    ?cs mu:uuid \"#{taxonomy}\" .
    ?bucket a mt:ScoreBucket .
    ?bucket mt:bucketFrom ?start .
    ?bucket mt:bucketTo   ?end .

    FILTER( (?score > ?start || ?start = 0) &&
      ?score <= ?end )
  }
}
}
GROUP BY ?bucket ?start ?end"
    sparqlResult = sparqlQuery query
    solutions_to_hash_list sparqlResult
  end

  def solution_to_hash (solution)
    hash = solution.to_hash
    hash.each do |key, value|
      hash[key] = value.value
    end
    hash
  end

  def solutions_to_hash_list (solutions)
    result = []
    solutions.each_solution do |solution|
      result.push solution_to_hash(solution)
    end
    result
  end

  def solutions_to_list (solutions)
    results = []
    solutions_to_hash_list(solutions).each do |hash|
      hash.each do |key, value|
        results.push value
      end
    end
    results
  end

  def get_normalizers (time)
    query = "PREFIX mt: <#{MT}>
SELECT DISTINCT ?normalizer ?normPred ?normMax
WHERE {
  GRAPH <#{settings.graph}> {
    ?normalizer a mt:Normalizer;
             mt:normalizeOn \"#{time}\" ;
             mt:normalizePred ?normPred ;
             mt:normalizeMax ?normMax.
  }
}"
    result = solutions_to_hash_list(sparqlQuery(query))
  end

  def normalize_scores (time = "start")
    normalizers = get_normalizers(time)
    normalizers.each do |normalizer|
      run_normalizer(normalizer)
    end
  end

  def get_norm_max(normalizer)
    predicate = normalizer[:normPred]
    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
SELECT max(?score) as ?max WHERE {
  GRAPH <#{settings.graph}> {
    ?mapping a mt:Mapping .
    ?mapping <#{predicate}> ?score .
  }
}"
    solutions_to_list(sparqlQuery(query))[0]
  end

  def run_normalizer (normalizer)
    predicate = normalizer[:normPred]
    max = normalizer[:normMax]

    if max == "max"
      max = get_norm_max(normalizer)
    end

    query = "PREFIX mt: <#{MT}>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX mu: <http://mu.semte.ch/vocabularies/>
DELETE {
  GRAPH <#{settings.graph}> {
    ?mapping <#{predicate}> ?score .
  }
} INSERT {
  GRAPH <#{settings.graph}> {
    ?mapping <#{predicate}> ?normScore .
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?mapping a mt:Mapping .
    ?mapping <#{predicate}> ?score .

    BIND ( IF((xsd:float(?score) / xsd:float(#{max})) > 1, 1, (xsd:float(?score) / xsd:float(#{max}))) AS ?normScore)
  }
}"
    sparqlUpdate(query)
  end

  def cleanup_mappings
    # need to remove contextScores with too low a rating, they don't add information
    minContext = settings.minContext
    query = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{settings.graph}> {
    ?target ?p ?o.
  }
}
where {
  GRAPH <#{settings.graph}> {
    ?target a mt:Mapping.
    ?target mt:mapsFrom ?from.
    ?target mt:mapsTo ?to.
    ?target mt:contextScore ?cscore.
    ?target ?p ?o.
    FILTER (?cscore < #{minContext})
  }
}"
    sparqlUpdate(query)

    # need to combine mappings with context score into score mappings
    query = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{settings.graph}> {
    ?other ?p ?o.
  }
}
INSERT{
  GRAPH <#{settings.graph}> {
    ?target mt:contextScore ?cscore.
  }
} where {
  GRAPH <#{settings.graph}> {
    ?target a mt:Mapping.
    ?target mt:mapsFrom ?from.
    ?target mt:mapsTo ?to.
    ?target mt:score ?score.
    ?other a mt:Mapping.
    ?other mt:mapsFrom ?from.
    ?other mt:mapsTo ?to.
    ?other mt:contextScore ?cscore.
    ?other ?p ?o.
    FILTER (?other != ?target)
  }
}"
    sparqlUpdate(query)

    # need to remove the duplicate mappings (e.g. if esco-to-esco mapping)
    query = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{settings.graph}> {
    ?other ?p ?o.
    ?s mt:score ?score.
    ?s mt:contextScore ?contextScore.
  }
}
INSERT {
  GRAPH <#{settings.graph}> {
    ?s mt:score ?t_score.
    ?s mt:contextScore ?t_contextScore.
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?s a mt:Mapping.
    ?other a mt:Mapping.
    ?s mt:mapsFrom ?from.
    ?s mt:mapsTo ?to.
    ?other mt:mapsFrom ?from.
    ?other mt:mapsTo ?to.
    ?other ?p ?o.
    FILTER (?s < ?other)

    OPTIONAL {
      ?s mt:score ?score.
    }
    OPTIONAL {
      ?s mt:contextScore ?contextScore.
    }

    OPTIONAL {
      ?other mt:score ?o_score.
    }
    OPTIONAL {
      ?other mt:contextScore ?o_contextScore.
    }

    BIND(IF(?score < ?o_score, ?o_score, ?score) as ?t_score)
    BIND(IF(?contextScore < ?o_contextScore, ?o_contextScore, ?contextScore) as ?t_contextScore)
  }
}"
    sparqlUpdate(query)

    # need to remove mappings from a concept to itself
    query = "PREFIX mt: <#{MT}>
DELETE {
  graph <#{settings.graph}> {
    ?s ?p ?o.
  }
} where {
  graph <#{settings.graph}> {
    ?s a mt:Mapping.
    ?s mt:mapsFrom ?target.
    ?s mt:mapsTo ?target.
    ?s ?p ?o.
  }
}"

    sparqlUpdate(query)
  end

  def get_flooders
    query = "PREFIX mt: <#{MT}>
SELECT DISTINCT ?flooder ?floodPath ?floodPred ?floodFrom ?floodTo ?floodToInv ?floodFromInv ?maxEvidence ?scorePred ?pathRestriction ?floodRestriction
WHERE {
  ?flooder a mt:Flooder;
           mt:scorePred ?scorePred ;
           mt:floodPred ?floodPred ;
           mt:maxEvidence ?maxEvidence .
  OPTIONAL {
    ?flooder mt:pathRestriction ?pathRestriction .
  }
  OPTIONAL {
    ?flooder mt:floodRestriction ?floodRestriction .
  }
  OPTIONAL {
    ?flooder mt:floodPath ?floodPath .
  }
  OPTIONAL {
    ?flooder mt:floodTo ?floodTo .
  }
  OPTIONAL {
    ?flooder mt:floodFrom ?floodFrom .
  }
  OPTIONAL {
    ?flooder mt:floodToInv ?floodToInv .
  }
  OPTIONAL {
    ?flooder mt:floodFromInv ?floodFromInv .
  }
}"
    result = solutions_to_hash_list(sparqlQuery(query))
  end

  def load_all_stopwords
    query = "PREFIX mt: <#{MT}>
SELECT ?conceptScheme ?file WHERE {
  GRAPH <#{settings.graph}> {
    ?settings mt:settingsFor ?conceptScheme .
    ?settings a mt:MappingSettings .
    ?settings mt:stopwordsFile ?file .
  }
}"
    result = solutions_to_hash_list(sparqlQuery(query))
    binding.pry
    result.each do |row|
      load_stopwords(row[:conceptScheme], row[:file])
    end
  end

  def load_stopwords (conceptscheme, file)
    query = "PREFIX mt: <#{MT}>
INSERT DATA {
GRAPH <#{settings.graph}> {\n"

    File.open(file).each do |line|
      query+= "<#{conceptscheme}/settings> mt:hiddenStopword \"#{line.strip}\" .\n"
    end

    query += "} }"
    sparqlUpdate(query)
  end

  def load_data
    binding.pry
    Dir.entries("./data").each do |file|
      path = "./data/#{file}"
      if File.file?(path)
        sparqlInsert(path)
      end
    end
  end

  def build_flooders
    flooders = get_flooders()
    flooders.each do |flooder|
      build_flood_paths(flooder)
    end
  end

  ###*
  # Flood paths are connections from one mapping to another along which the given flooder is activated.
  # These paths are built for all flooders on startup, as they only need to be built once and should
  # provide a performance increase in the queries. The flood paths are stored in a graph for the flooder
  # the graph uri is build by taking the flooder uri and appending /graph to it.
  ###
  def build_flood_paths (flooder)
    flooderURI = flooder[:flooder]
    floodFrom = flooder[:floodFrom] || flooder[:path] || "<http://www.w3.org/2004/02/skos/core#broader>/^<http://www.w3.org/2004/02/skos/core#broader>"
    floodTo = flooder[:floodTo] || flooder[:path] || "<http://www.w3.org/2004/02/skos/core#broader>/^<http://www.w3.org/2004/02/skos/core#broader>"
    floodToInv = flooder[:floodToInv]
    floodFromInv = flooder[:floodFromInv]

    cleanup = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{flooderURI}/graph> {
    ?target mt:floodFrom ?origin .
  }
} WHERE {
  GRAPH <#{flooderURI}/graph> {
    ?target mt:floodFrom ?origin .
  }
}"
    sparqlUpdate cleanup

    restriction = flooder[:pathRestriction] || ""
    floodToInvQ = ""
    if floodToInv
      floodToInvQ = "UNION {?to #{floodToInv} ?floodTo.}"
    end
    floodFromInvQ = ""
    if floodFromInv
      floodFromInvQ = "UNION {?to #{floodFromInv} ?floodTo.}"
    end

    flooding = "          {{?floodTo #{floodTo} ?to.} #{floodToInvQ}}
          {{?floodFrom #{floodFrom} ?from.} #{floodFromInvQ}}
"
    query = "PREFIX mt: <#{MT}>
INSERT {
  GRAPH <#{flooderURI}/graph> {
    ?target mt:floodFrom ?flooder .
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?target a mt:Mapping;
            mt:mapsFrom ?from;
            mt:mapsTo ?to.
    ?flooder a mt:Mapping;
             mt:mapsFrom ?floodFrom;
             mt:mapsTo ?floodTo.
    #{restriction}
    #{flooding}
  }
}"
    sparqlUpdate query
  end

  def clean_flooder (flooder)
    pred = flooder[:scorePred] || MT.floodScore
    cleanup = "PREFIX mt: <#{MT}>
DELETE {
  GRAPH <#{settings.graph}> {
    ?target <#{pred}> ?oldscore .
  }
} WHERE {
  GRAPH <#{settings.graph}> {
    ?target <#{pred}> ?oldscore .
  }
}"
    sparqlUpdate cleanup
  end

  def flood_scores (flooder)
    flooderURI = flooder[:flooder]
    pred = flooder[:scorePred] || MT.floodScore
    maxEvidence = flooder[:maxEvidence] || 100
    floodPred = flooder[:floodPred] || MT.score
    restriction = flooder[:floodRestriction] || ""

    query = "PREFIX mt: <#{MT}>
INSERT {
  GRAPH <#{settings.graph}> {
    ?target <#{pred}> ?newscore .
  }
} WHERE {
  ?target a mt:Mapping.
  { SELECT ?target IF((?count > #{maxEvidence}), (?avg), (xsd:float(?avg)*(xsd:float(?count)/xsd:float(#{maxEvidence})))) AS ?newscore ?min ?avg ?max ?count WHERE {
      SELECT ?target MIN(?score) as ?min AVG(?score) as ?avg MAX(?score) as ?max COUNT(DISTINCT ?flooder) as ?count WHERE {
        GRAPH <#{settings.graph}> {
          ?target a mt:Mapping.
          ?flooder a mt:Mapping;
                   <#{floodPred}> ?score.
          #{restriction}
        }
        GRAPH <#{flooderURI}/graph> {
          ?target mt:floodFrom ?flooder .
        }
      } GROUP BY ?target
    }
  }
}"
    sparqlUpdate query
  end

  def sparqlQuery (query)
    puts "Running query \n#{query}"
    start = Time.now

    result = settings.sparql_client.query query

    finish = Time.now
    duration = (finish - start)*1000
    puts "done (#{duration} ms)"

    result
  end

  def sparqlUpdate (query)
    puts "Running query \n#{query}"
    start = Time.now

    result = settings.sparql_client.update query

    finish = Time.now
    duration = (finish - start)*1000
    puts "done (#{duration} ms)"

    result
  end

  def sparqlConstruct (query)
    puts "Running query \n#{query}"
    start = Time.now

    result = settings.sparql_client.response( query, :content_type => "application/rdf+xml")
    graph = RDF::Graph.new
    graph << RDF::Reader.for(:content_type => "application/rdf+xml").new(result.body.to_str)

    finish = Time.now
    duration = (finish - start)*1000
    puts "done (#{duration} ms)"

    graph
  end

  def sparqlInsert (file)
    puts "loading #{file}"
    start = Time.now
    data = RDF::Graph.load("#{file}")
    puts "Inserting #{data.count} statements"

    settings.sparql_client.insert_data( data, :graph => settings.graph )

    finish = Time.now
    duration = (finish - start)*1000
    puts "done (#{duration} ms)"
  end

end
