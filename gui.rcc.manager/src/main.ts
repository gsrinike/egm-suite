import { createApp } from 'vue';
import App from './App.vue';
import '@egm/gui.common/src/styles.css';
import '@egm/gui.cnm.manager/src/styles.css';
import './styles.css';
import { loadAppConfig } from './config/appConfig';

void loadAppConfig().finally(() => {
  createApp(App).mount('#app');
});
