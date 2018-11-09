import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { MigrationHistory } from 'app/shared/model/migration-history.model';
import { MigrationHistoryService } from './migration-history.service';
import { MigrationHistoryComponent } from './migration-history.component';
import { MigrationHistoryDetailComponent } from './migration-history-detail.component';
import { IMigrationHistory } from 'app/shared/model/migration-history.model';

@Injectable({ providedIn: 'root' })
export class MigrationHistoryResolve implements Resolve<IMigrationHistory> {
    constructor(private service: MigrationHistoryService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((migrationHistory: HttpResponse<MigrationHistory>) => migrationHistory.body));
        }
        return of(new MigrationHistory());
    }
}

export const migrationHistoryRoute: Routes = [
    {
        path: 'migration-history',
        component: MigrationHistoryComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migrationHistory.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'migration-history/:id/view',
        component: MigrationHistoryDetailComponent,
        resolve: {
            migrationHistory: MigrationHistoryResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migrationHistory.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];
