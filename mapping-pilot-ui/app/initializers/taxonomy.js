import Service from '../services/taxonomies';
import Model from '../models/taxonomy';
import Adapter from '../adapters/taxonomy';
import Serializer from '../serializers/taxonomy';

export function initialize(container, application) {
  const adapter = 'service:taxonomies-adapter';
  const serializer = 'service:taxonomies-serializer';
  const service = 'service:taxonomies';
  const model = 'model:taxonomies';

  application.register(model, Model, { instantiate: false, singleton: false });
  application.register(service, Service);
  application.register(adapter, Adapter);
  application.register(serializer, Serializer);

  application.inject('service:store', 'taxonomies', service);
  application.inject(service, 'serializer', serializer);
}

export default {
  name: 'taxonomies-service',
  after: 'store',
  initialize: initialize
};
