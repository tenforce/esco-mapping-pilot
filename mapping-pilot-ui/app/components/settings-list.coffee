`import Ember from 'ember'`
`import ClickElsewhereMixin from '../mixins/click-elsewhere'`

SettingsListComponent = Ember.Component.extend ClickElsewhereMixin,
  classNames:["z-depth-1", "deep-orange"]
  onClickElsewhere: ->
    @sendAction "closeSettings"
  actions:
    logout: ->
      @sendAction "logout"

`export default SettingsListComponent`
