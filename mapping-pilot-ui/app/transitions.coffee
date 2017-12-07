stdMovementDuration = 300
slowMovementDuration = stdMovementDuration * 2

transitionMap = ->
  @transition @fromRoute('taxonomies'),
    @toRoute('taxonomies.matches'),
    @use('toLeft', {duration: slowMovementDuration}),
    @reverse('toRight', {duration: slowMovementDuration})
    @debug()
  @transition @matchSelector('.game-score .matches-to-do *, .game-score .matches-found *, .game-score .matches-denied *'),
    @use('toUp')

`export default transitionMap`
