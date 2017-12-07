`import Ember from 'ember'`
`import ClickElsewhereMixin from '../../../mixins/click-elsewhere'`
`import { module, test } from 'qunit'`

module 'ClickElsewhereMixin'

# Replace this with your real tests.
test 'it works', (assert) ->
  ClickElsewhereObject = Ember.Object.extend ClickElsewhereMixin
  subject = ClickElsewhereObject.create()
  assert.ok subject
