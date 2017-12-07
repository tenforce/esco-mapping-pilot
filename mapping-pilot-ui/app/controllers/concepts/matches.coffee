`import Ember from 'ember'`
`import TaxonomiesMatchesController from '../taxonomies/matches'`

ConceptMatchesController = TaxonomiesMatchesController.extend
  minScore: Ember.computed.alias 'model.taxonomy.minscore'


`export default ConceptMatchesController`
