`import Ember from 'ember'`

#original attribution https://gist.github.com/alexspeller/6251054

ClickElsewhereMixin = Ember.Mixin.create
  onClickElsewhere: Ember.K

  clickHandler: (->
    @get('elsewhereHandler').bind @
  ).property()

  elsewhereHandler: (event) ->
    isThisElement = $(event.target).closest(@get('element')).length is 1
    unless isThisElement
      @onClickElsewhere event

  didInsertElement: ->
    @_super()
    $(window).bind 'click', @get("clickHandler")

  willDestroy: ->
    $(window).unbind 'click', @get("clickHandler")
    @_super()


`export default ClickElsewhereMixin`
