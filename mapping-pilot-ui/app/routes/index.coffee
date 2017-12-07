`import Ember from 'ember'`

IndexRoute = Ember.Route.extend
  activate: ->
    @send 'updateTitle', "Mapping Tool"


`export default IndexRoute`
