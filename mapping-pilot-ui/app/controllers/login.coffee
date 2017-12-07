`import Ember from 'ember'`

LoginController = Ember.Controller.extend
  username: null
  password: null
  actions:
    login: ->
      user = @get 'user'
      username = @get 'username'
      password = @get 'password'
      user.set 'username', username
      user.set 'password', password

      base = document.baseURI.replace("http://", "http://#{username}:#{password}@")
      Ember.$.ajax
        url: "#{base}api/flood_status"
        type: "POST"
        success: =>
          @transitionToRoute 'index'
        error: (error) =>
          if error.status == 401
            alert "Could not log in using this combination of username and password"
            user.logout()
          else
            @transitionToRoute 'taxonomies'
      

`export default LoginController`
