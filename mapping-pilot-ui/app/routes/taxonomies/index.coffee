`import Ember from 'ember'`
`import AuthRoute from '../../utils/auth-route'`

TaxonomiesIndexRoute = AuthRoute.extend
  activate: ->
    @_super(arguments...)
    @send 'updateTitle', "Taxonomy List"
  model: ->
    @store.find('taxonomies').then (result) ->
      result.sort (one,two) ->
        one.get('name') > two.get('name')


`export default TaxonomiesIndexRoute`
