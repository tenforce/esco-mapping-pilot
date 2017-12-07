`import Ember from 'ember'`

UserService = Ember.Object.extend
  init: ->
    @_super(arguments...)
    @set 'AuthorizationHeader', localStorage['AuthorizationHeader']
  username: null
  password: null
  loggedIn: Ember.computed 'username', 'password', 'AuthorizationHeader', ->
    (@get('username') and @get('password')) or @get('AuthorizationHeader')
  authenticationObserver: Ember.observer 'username', 'password', (->
    username = @get 'username'
    password = @get 'password'
    if username and password
      header = "Basic " + btoa(username + ":" + password)
      localStorage['AuthorizationHeader'] = header
    Ember.$.ajaxSetup
      beforeSend: (xhr) ->
        header = localStorage['AuthorizationHeader']
        xhr.setRequestHeader("Authorization", header)
  ).on('init')
  logout: ->
    @set 'username', null
    @set 'password', null
    delete localStorage['AuthorizationHeader']
    Ember.$.ajaxSetup
      beforeSend: (xhr) -> {}        

`export default UserService`
