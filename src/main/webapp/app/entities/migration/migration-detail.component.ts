import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMigration } from 'app/shared/model/migration.model';

@Component({
    selector: 'jhi-migration-detail',
    templateUrl: './migration-detail.component.html',
    styleUrls: ['./migration-detail.component.css']
})
export class MigrationDetailComponent implements OnInit {
    migration: IMigration;

    constructor(private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ migration }) => {
            this.migration = migration;
        });
    }

    previousState() {
        window.history.back();
    }
}
