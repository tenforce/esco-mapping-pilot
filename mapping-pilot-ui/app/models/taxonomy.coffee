`import Ember from 'ember'`
`import Resource from './resource'`
`import { attr, hasOne, hasMany } from 'ember-jsonapi-resources/models/resource'`

Taxonomy = Resource.extend
  type: 'taxonomies'
  service: Ember.inject.service 'taxonomies'

  name: attr()
  minscore: attr()
  description: attr()
  kind: attr()

  topics: hasMany('topics')


`export default Taxonomy`
