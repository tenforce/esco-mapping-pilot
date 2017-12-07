`import Ember from 'ember'`
`import AuthRoute from '../../utils/auth-route'`

TaxonomiesManageRoute = AuthRoute.extend
  model: (params) ->
    @store.find('taxonomy', id: params.id).then (result) =>
      result.store = @store
      result
  afterModel: (model) ->
    Ember.run.next =>
      @send 'updateTitle', "Fine-tune '#{model.get('name')}' Mappings"


`export default TaxonomiesManageRoute`
