`import Ember from 'ember'`
`import config from './config/environment'`

Router = Ember.Router.extend
  location: config.locationType

Router.map ->
  @resource 'taxonomies', path: '/schemes', ->
    @route 'show', path: ":id/show"
    @route 'matches', path: ":id/matches"
    @route 'manage', path: ":id/manage"
    @route 'statistics', path: ":id/stats"
  @route 'login'
  @resource 'concepts', path: '/concepts', ->
    @route 'matches', path: ":id/matches"

`export default Router`
