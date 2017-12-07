`import Ember from 'ember'`

StopwordsDialogComponent = Ember.Component.extend
  loading: false
  init: ->
    @_super(arguments...)
    @loadStopwords()
  classNames: ['stopwords-dialog', 'z-depth-1'],
  stopwordList: Ember.ArrayProxy.create(content:[])
  didInsertElement: ->
    @_super(arguments...)
    Ember.run.later (=> @$('input').focus()), 500
  addStopword: (stopword) ->
    stopwordList = @get 'stopwordList'
    if stopwordList.findBy('label', stopword)
      return
    stopwordList.pushObject Ember.Object.create
      label: stopword
  stopwordsObserver: Ember.observer 'stopwords', ->
    stopwords = @get 'stopwords'
    firstStopword = stopwords.substring(0, stopwords.indexOf(',')).trim()
    if firstStopword
      @addStopword(firstStopword)
      @set 'stopwords', stopwords.substring(stopwords.indexOf(',')+1)
  loadStopwords: ->
    @set 'loading', true
    Ember.$.ajax
      url: "/api/taxonomy/#{@get('taxonomy')}/stopwords?hidden=#{!!@get('hidden')}"
      success: (result) =>
        list = []
        JSON.parse(result).map (stopword) ->
          list.push {label: stopword}
        list.sort (a,b) ->
          if a.label > b.label
            1
          else
            -1
        @set 'stopwordList', Ember.ArrayProxy.create(content:list)

      error: ->
        alert 'Sorry, could not fetch stopword list'
      complete: =>
        @set 'loading', false

  saveStopwords: ->
    promise = new Ember.RSVP.Promise (resolve,reject) =>
      stopwords = []
      seen = {}
      @get('stopwordList').map (item) ->
        if not item.removed and not seen[item.label]
          stopwords.push item.label
          seen[item.label] = true
  
      Ember.$.ajax
        url: "/api/taxonomy/#{@get('taxonomy')}/stopwords"
        data: JSON.stringify(stopwords: stopwords)
        type: 'POST'
        complete: ->
          resolve()
  closeAndSave: ->
    @saveStopwords().then =>
      @sendAction 'closeDialog'
  observeHidden: Ember.observer 'hidden', ->
    @saveStopwords().then =>
      @loadStopwords()
  actions:
    addStopwords: ->
      if @get('loading')
        return
      stopwords = @get 'stopwords'
      if stopwords.trim().length == 0
        @saveStopwords()
      else
        stopwords.split(',').map (stopword) =>
          @addStopword stopword.trim()
        @set 'stopwords', ""
    removeStopword: (stopword) ->
      Ember.set(stopword, 'removed', not Ember.get(stopword, 'removed'))
    saveStopwords: ->
      if @get('loading')
        return
      @closeAndSave()


`export default StopwordsDialogComponent`
