`import Ember from 'ember'`
`import TaxonomiesMatchController from './matches'`

TaxonomiesManageController = TaxonomiesMatchController.extend
  visibilitySettings: Ember.computed 'settings.visibilitySettings', 'settings.visibilities', ->
    result = []
    visibilities = @get 'settings.visibilities'
    visibilities = visibilities.manage
    result = []
    @get('settings.visibilitySettings').map (item) ->
       result.push(Ember.merge(Ember.Object.create({value: visibilities[item.get('type')]}),item))
    result


`export default TaxonomiesManageController`
