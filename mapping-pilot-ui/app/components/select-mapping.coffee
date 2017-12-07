`import Ember from 'ember'`

SelectMappingComponent = Ember.Component.extend
  classNames: ["select-mapping card"]
  taxonomies: []
  topicObserver: Ember.observer 'origin', (->
    taxonomy = @get 'origin.taxonomy'
    store = @get 'store'
    selected = null
    store.find('taxonomies').then (allTaxonomies) =>
      taxonomies = []
      allTaxonomies.map (tax) ->
        if tax.get('id') != taxonomy.get('id') and tax.get('kind') == taxonomy.get('kind')
          selected ||= tax
          taxonomies.push tax
      @set 'taxonomies', taxonomies
      @set 'targetTaxonomy', selected
  ).on('init')
  targetTaxonomy:null
  selectedTaxonomyObserver: Ember.observer 'targetTaxonomy', (->
    mappings = @get('origin.mappingsFrom')
    selection = mappings.map (mapping) ->
      mapping.get 'to.id'
    @set 'originalSelection', selection
    @set 'selection', Ember.ArrayProxy.create(content:selection.concat([]))
  ).on('init')
  selectedTaxonomy: Ember.computed.notEmpty 'targetTaxonomy.name'
  actions:
    done: ->
      selected = @get 'selection'
      original = @get 'originalSelection'

      newItems = []
      selected.map (item) ->
        if original.indexOf(item) < 0
          newItems.push item
      @sendAction 'selectedMappings', newItems
    cancel: ->
      @sendAction 'selectedMappings', []
      

`export default SelectMappingComponent`
