`import Ember from 'ember'`
`import settings from '../../utils/mapping-settings'`

TaxonomiesStatisticsController = Ember.Controller.extend
  init: ->
    @_super(arguments...)
    @set 'settings', settings
  advanced: true
  chartOptions:
    responsive: true,
    showTooltips: true,
    tooltipTemplate: "<%= value %>",
    showTooltips: true
    tooltipEvents: ["mousemove", "touchstart", "touchmove"]
    tooltipFillColor: "rgba(0,0,0,0.8)",
    scaleShowVertialLines: false
    barValueSpacing: 1
  minScore: Ember.computed.alias 'model.taxonomy.minscore'
  possibleMatches: Ember.computed 'model.conceptCounts', ->
    from = parseInt(@get 'model.conceptCounts.fromCount')
    to = parseInt(@get 'model.conceptCounts.toCount')

    from*to
  matchesTodo: Ember.computed 'model.matchCounts.yes', 'chartStats.correctedScore.considered', ->
    parseInt(@get('chartStats.correctedScore.considered'))-parseInt(@get('model.matchCounts.yes'))
  reducedWorkload: Ember.computed 'possibleMatches', 'chartStats.correctedScore.considered', ->
    possible = @get('possibleMatches')
    considered = @get('chartStats.correctedScore.considered')
    ((1-(considered/possible)).toFixed(2)*100).toFixed(0)
  advancedChartNames: ['fullScore', 'floodScore', 'scoreToFlood', 'contextScore', 'baseScore' ]
  advancedCharts: Ember.computed 'advancedChartNames', 'chartStats', ->
    model = @get('chartStats')
    chartNames = @get 'advancedChartNames'
    chartNames.map (name) =>
      chart = model.get "#{name}"
  chartStats: Ember.computed 'model', 'minScore', 'model.matchCount.no', ->
    results = @get 'model'
    if not results.correctedScore
      @send 'refreshModel'
      return null
    chartStats = Ember.Object.create()
    Ember.keys(results).map (stat) =>
      result = results.get("#{stat}.value")
      if not result
        return
      avg = 0
      total = 0
      dropped = 0
      minScore = @get 'minScore'
      result.map (one) =>
        count = parseFloat(one.count)
        start = parseFloat(one.start)
        end = parseFloat(one.end)
        avg+=(count*(start + (end - start)))
        if start < minScore
          dropped+=count
        total+= count
      additional = 0
      if minScore > 0
        additional = parseInt(@get('model.matchCounts.no'))
      chartStats.set(stat, Ember.Object.create())
      chartStats.set("#{stat}.dropped", dropped)
      chartStats.set("#{stat}.name", stat)
      chartStats.set("#{stat}.title", results.get("#{stat}.title"))
      chartStats.set("#{stat}.hint", results.get("#{stat}.hint"))
      chartStats.set("#{stat}.data", @buildChartData(result))
      chartStats.set("#{stat}.total", total)
      chartStats.set("#{stat}.considered", total+additional-dropped)
      if result and result.length > 0
        chartStats.set("#{stat}.avg", (avg/total).toFixed(2))
        chartStats.set("#{stat}.hasData", true)
      else
        chartStats.set("#{stat}.avg", 0)
        chartStats.set("#{stat}.hasData", false)
    chartStats
  buildChartData: (rawData) ->
    buckets = rawData or []
    # if not buckets
    #   # sad, but apparently we don't get the right model at first
    #   Ember.run.debounce this, @refresh, 100
    #   return @get 'emptyDataset'
    labels = []
    buckets.sort (a, b) ->
      a.start - b.start
    newBuckets = []
    index = 0
    while index < 50
      newBuckets.push {start: (0.02*index).toFixed(2), end: (0.02*(index+1)).toFixed(2), count: 0}
      index++
    index = 0
    buckets.map (bucket) ->
      while (parseFloat(newBuckets[index].start)).toFixed(2) < parseFloat(bucket.start).toFixed(2)
        index++
      newBuckets[index].count = bucket.count
      index++
    buckets = newBuckets
    count = 0
    data = buckets.map (bucket) ->
      # hackerdihackhack
      if count % 5 > 0 && count != buckets.length-1
        labels.push ""
      else if count < buckets.length-1
        labels.push bucket.start
      else
        labels.push bucket.end
      count++
      bucket.count

    result =
      labels: labels
      datasets: [
        {
          label: "counts"
          fillColor: "rgba(255, 152, 0,0.8)"
          strokeColor: "rgba(255, 87, 34,0.8)"
          highlightFill: "rgba(255, 87, 34,0.75)"
          highlightStroke: "rgba(255, 87, 34,1)"
          data: data
        }
      ]
  emptyDataset:
    labels: []
    datasets: [
      {
        label: "counts"
        fillColor: "rgba(255, 152, 0,0.8)"
        strokeColor: "rgba(255, 87, 34,0.8)"
        highlightFill: "rgba(255, 87, 34,0.75)"
        highlightStroke: "rgba(255, 87, 34,1)"
        data: []
      }
    ]
  actions:
    toggleAdvanced: ->
      @set 'advanced', not @get('advanced')

`export default TaxonomiesStatisticsController`
