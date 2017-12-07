`import Ember from 'ember'`
`import AuthRoute from '../../utils/auth-route'`

TaxonomiesShowRoute = AuthRoute.extend
  activate: ->
    @_super(arguments...)
    @notifyPropertyChange 'model'
  model: (params) ->
    new Ember.RSVP.Promise (resolve, reject) =>
      Ember.$.ajax
        url: "/api/taxonomy/#{params.id}/matchCounts"
        success: (result) =>
          result = JSON.parse(result)
          result.id = params.id
          @store.find('taxonomy', id: params.id).then (taxonomy) =>
            result.taxonomy = taxonomy
            resolve(result)
        error: ->
          alert 'Sorry, could not load the taxonomy statistics'
          resolve([])
  afterModel: (model) ->
    Ember.run.next =>
      @send 'updateTitle', "'#{model.taxonomy.get('name')}' Mapping Results"


`export default TaxonomiesShowRoute`
