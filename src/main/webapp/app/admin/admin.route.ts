import { Routes } from '@angular/router';
import { auditsRoute, configurationRoute, docsRoute, healthRoute, logsRoute, metricsRoute } from './';

const ADMIN_ROUTES = [auditsRoute, configurationRoute, docsRoute, healthRoute, logsRoute, metricsRoute];

export const adminState: Routes = [
    {
        path: '',
        data: {
            authorities: ['ROLE_ADMIN']
        },
        children: ADMIN_ROUTES
    }
];
