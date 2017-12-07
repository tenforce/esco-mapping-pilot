import Service from '../services/topics';
import Model from '../models/topic';
import Adapter from '../adapters/topic';
import Serializer from '../serializers/topic';

export function initialize(container, application) {
  const adapter = 'service:topics-adapter';
  const serializer = 'service:topics-serializer';
  const service = 'service:topics';
  const model = 'model:topics';

  application.register(model, Model, { instantiate: false, singleton: false });
  application.register(service, Service);
  application.register(adapter, Adapter);
  application.register(serializer, Serializer);

  application.inject('service:store', 'topics', service);
  application.inject(service, 'serializer', serializer);
}

export default {
  name: 'topics-service',
  after: 'store',
  initialize: initialize
};
