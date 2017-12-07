`import Ember from 'ember'`

TaxonomiesShowController = Ember.Controller.extend
  hasMatches: Ember.computed.notEmpty 'model'
  downloadStringAsFile: (string,name) ->
    element = document.createElement('a')
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(string))
    element.setAttribute('download', name)
    
    element.style.display = 'none';
    document.body.appendChild(element);
    
    element.click();
    
    document.body.removeChild(element);
  actions:
    downloadRDF: ->
      id = @get('model.id')
      Ember.$.ajax
        url: "/api/taxonomy/#{id}/mappings"
        headers:
          Accept : "application/rdf+xml"
        type: "GET"
        success: (result) =>
          @downloadStringAsFile(new XMLSerializer().serializeToString(result),"mappings.rdf")
        error: ->
          alert 'could not download rdf mappings, please contact your administrator'
    downloadCSV: ->
      id = @get('model.id')
      Ember.$.ajax
        url: "/api/taxonomy/#{id}/mappings"
        headers:
          Accept : "text/csv",
        type: "GET"
        success: (result) =>
          @downloadStringAsFile(result,"mappings.csv")
        error: ->
          alert 'could not download csv mappings, please contact your administrator'
    clickTopic: (topic) ->
      @transitionToRoute 'concepts.matches', Ember.get(topic, 'id')
`export default TaxonomiesShowController`
