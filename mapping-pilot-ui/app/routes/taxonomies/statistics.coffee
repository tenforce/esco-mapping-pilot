`import Ember from 'ember'`
`import promiseAll from '../../utils/promise-all'`
`import settings from '../../utils/mapping-settings'`
`import AuthRoute from '../../utils/auth-route'`

TaxonomiesStatisticsRoute = AuthRoute.extend
  init: ->
    @_super(arguments...)
    @set 'settings', settings
  model: (params) ->

    promises = []
    id= params.id

    stats = 
      baseScore: Ember.Object.create
        pred:  "http://sem.tenforce.com/vocabularies/mapping-pilot/score"
        title: "Text Based Mapping"
        hint: "The raw ratings, purely based on textual similarity of the concepts"
      fullScore: Ember.Object.create
        pred:  "http://sem.tenforce.com/vocabularies/mapping-pilot/originalScore"
        title: "Before Human Validation (incl. flooding)"
        hint: "The generated ratings as the user sees them, including all smart adjustments"
      contextScore: Ember.Object.create
        pred:  "http://sem.tenforce.com/vocabularies/mapping-pilot/contextScore"
        title: "Context Based Mapping"
        hint: "The contextual ratings of the concepts based on wikipedia articles"
      floodScore: Ember.Object.create
        pred:  "http://sem.tenforce.com/vocabularies/mapping-pilot/floodScore"
        title: "Net result of Flooding"
        hint: "Flooded ratings of the concepts, using concept relations like skills"
      skillFloodInv: Ember.Object.create
        pred:  "http://sem.tenforce.com/vocabularies/mapping-pilot/skillFloodScore"
        title: "Net result of skill Flooding"
        hint: "Flooded ratings of occupations based on skills"
      scoreToFlood: Ember.Object.create
        pred:  "http://sem.tenforce.com/vocabularies/mapping-pilot/scoreToFlood"
        title: "Combined Scores: textual + context"
        hint: "Combination of the textual and contextual scores of matches"
      correctedScore: Ember.Object.create
        pred:  "http://sem.tenforce.com/vocabularies/mapping-pilot/combinedScore"
        title: "Current Match Distribution"
        hint: "The final ratings, including automatic ratings and user ratings"
    objectiveStats = Ember.Object.create stats
    Object.keys(stats).map (key) =>
      stat = stats[key]
      pred = stat.pred
      
      p = new Ember.RSVP.Promise (resolve, reject) =>
        Ember.$.ajax
          url: "/api/taxonomy/#{id}/distribution"
          data:
            score_pred: pred
          success: (result) =>
            result = JSON.parse(result)
            objectiveStats.set("#{key}.value", result)
          error: ->
            objectiveStats.set("#{key}.value", [])
            alert 'Sorry, could not load the taxonomy statistics'
          complete: ->
            objectiveStats.set("#{key}.done", true)
            resolve()
      promises.push p
    promises.push @getStatistics(id,objectiveStats)
    promises.push @getTaxonomy(id, objectiveStats)
    promiseAll(promises).then =>
      Ember.run.next =>
        @notifyPropertyChange "model"
      objectiveStats
  afterModel: (model) ->
    Ember.run.next =>
      @send 'updateTitle', "Statistics for '#{model.get('taxonomy.name')}'"

  getTaxonomy: (id, stats) ->
    @store.find('taxonomy', id: id).then (result) =>
      Ember.set stats, 'taxonomy', result
      result.store = @store
      @notifyPropertyChange 'model'
  getStatistics: (id, stats) ->
    new Ember.RSVP.Promise (resolve,reject) =>
      Ember.$.ajax
        url: "/api/taxonomy/#{id}/statistics"
        type: 'GET'
        success: (result) ->
          result = JSON.parse(result)
          matches =
            yes: 0
            no: 0
            todo: 0

          result.matches.map (count) ->
            matches[count.kind] = parseInt(count.count)
          concepts = result.concepts
          Ember.keys(concepts).map (key) ->
            concepts[key] = parseInt(concepts[key])
          stats.set 'conceptCounts', concepts
          stats.set 'nomatches', result.nomatches
          stats.set 'nosuggestions', result.nosuggestions

          stats.set 'matchCounts', matches
          resolve()
        error: ->
          stats.set 'counts', {}
          alert 'sorry, could not load taxonomy statistics'
          resolve()
  actions:
    refreshModel: ->
      @refresh()
`export default TaxonomiesStatisticsRoute`
