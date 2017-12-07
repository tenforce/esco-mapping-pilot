`import Ember from 'ember'`
`import Resource from './resource'`
`import { attr, hasOne, hasMany } from 'ember-jsonapi-resources/models/resource'`

Topic = Resource.extend
  type: 'topics'
  service: Ember.inject.service('topics')

  name: attr()
  labels: attr()
  enlabels: attr()
  description: attr()
  uri: attr()

  taxonomy: hasOne('taxonomy')
  topics: hasMany('topics')
  mappingsFrom: hasMany({resource:'mappingsFrom', type:'mappings'})

  # /*
  # title: attr(),
  # date: attr(),

  # author: hasOne('author'),
  # comments: hasMany('comments')
  # */

`export default Topic`
