`import { test, moduleForModel } from 'ember-qunit'`

moduleForModel 'serializer:taxonomy',
  # Specify the other units that are required for this test.
  needs: ['serializer:taxonomy']

# Replace this with your real tests.
test 'it serializes records', (assert) ->
  record = @subject()

  serializedRecord = record.serialize()

  assert.ok serializedRecord
