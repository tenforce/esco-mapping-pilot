`import Ember from 'ember'`
`import AuthRoute from '../../utils/auth-route'`

ConceptMatchesRoute = AuthRoute.extend
  model: (params) ->
    @store.find('topics', id: params.id).then (result) =>
      result.store = @store
      result
  afterModel: (model) ->
    Ember.run.next =>
      @send 'updateTitle', "Mappings for '#{model.get('name')}'"


`export default ConceptMatchesRoute`
