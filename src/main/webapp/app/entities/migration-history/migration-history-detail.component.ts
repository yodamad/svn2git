import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMigrationHistory } from 'app/shared/model/migration-history.model';

@Component({
    selector: 'jhi-migration-history-detail',
    templateUrl: './migration-history-detail.component.html'
})
export class MigrationHistoryDetailComponent implements OnInit {
    migrationHistory: IMigrationHistory;

    constructor(private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ migrationHistory }) => {
            this.migrationHistory = migrationHistory;
        });
    }

    previousState() {
        window.history.back();
    }
}
