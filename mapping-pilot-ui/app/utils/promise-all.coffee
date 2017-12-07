###*
# Resolves when all promises in toResolve have been resolved
#
# options is an optional object with extra parameters:
# - dontCare: boolean, defaults to false. Resolve promise even if one of toResolve fails.
#   In that case, it still waits until all promises were resolved or rejected.
###
promiseAll = (toResolve, options) ->
  new Ember.RSVP.Promise (resolve, reject) ->
    allResults = []

    if toResolve.length == 0
      return resolve( allResults )

    count = 0
    handlePromise = (result) ->
      count++
      allResults.pushObject( result )
      if count == toResolve.length
        resolve( allResults )
    rejected = false
    toResolve.map (promise) ->
      onFail = (error) ->
        if not rejected
          rejected = true
          reject(error)
      if options?.dontCare
        onFail = handlePromise
      promise.then(handlePromise).catch(onFail)

`export default promiseAll`
