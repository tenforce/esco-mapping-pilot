import Service from '../services/mappings';
import Model from '../models/mapping';
import Adapter from '../adapters/mapping';
import Serializer from '../serializers/mapping';

export function initialize(container, application) {
  const adapter = 'service:mappings-adapter';
  const serializer = 'service:mappings-serializer';
  const service = 'service:mappings';
  const model = 'model:mappings';

  application.register(model, Model, { instantiate: false, singleton: false });
  application.register(service, Service);
  application.register(adapter, Adapter);
  application.register(serializer, Serializer);

  application.inject('service:store', 'mappings', service);
  application.inject(service, 'serializer', serializer);
}

export default {
  name: 'mappings-service',
  after: 'store',
  initialize: initialize
};
