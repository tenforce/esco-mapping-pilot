import ApplicationAdapter from './application';
//import config from 'mapping-pilot/config/environment';

export default ApplicationAdapter.extend({
  type: 'topic',

  url: /*config.APP.API_PATH + */ '/topics',

  /*fetchUrl: function(url) {
    const proxy = config.APP.API_HOST_PROXY;
    const host = config.APP.API_HOST;
    if (proxy && host) {
      url = url.replace(proxy, host);
    }
    return url;
  }*/
});