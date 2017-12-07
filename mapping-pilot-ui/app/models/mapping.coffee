`import Ember from 'ember'`
`import Resource from './resource'`
`import { attr, hasOne, hasMany } from 'ember-jsonapi-resources/models/resource'`

Mapping = Resource.extend
  type: 'mappings'
  service: Ember.inject.service 'mappings'

  score: attr()
  originalscore: attr()
  matchtype: attr()

  from: hasOne {resource: 'from', type: 'topic'}
  to: hasOne {resource: 'to', type: 'topic'}


`export default Mapping`
