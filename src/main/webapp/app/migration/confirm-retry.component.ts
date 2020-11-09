import { Component, Inject, OnInit } from '@angular/core';

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import { MigrationService } from 'app/entities/migration';
import { ConfigurationService } from 'app/shared/service/configuration-service';

@Component({
    selector: 'jhi-confirm-retry-modal',
    templateUrl: './confirm-retry.component.html',
    styleUrls: ['./migration-check.component.css']
})
export class JhiConfirmRetryModalComponent implements OnInit {
    // Flags
    forceRemoveGroup = false;
    cleaning = false;
    errorCleaning = false;
    authorizeCleaning = false;

    constructor(
        public dialogRef: MatDialogRef<JhiConfirmRetryModalComponent>,
        private _migrationService: MigrationService,
        private _configurationService: ConfigurationService,
        @Inject(MAT_DIALOG_DATA) public data: any
    ) {}

    ngOnInit(): void {
        this._configurationService
            .flagProjectCleaning()
            .subscribe(res => (this.authorizeCleaning = res), err => (this.authorizeCleaning = false));
    }

    retry() {
        this.cleaning = true;
        this.errorCleaning = false;
        this._migrationService.retry(this.data.migId, this.forceRemoveGroup).subscribe(
            res => {
                this.dialogRef.close(res.body);
                this.cleaning = false;
            },
            err => {
                this.cleaning = false;
                this.errorCleaning = true;
                this.forceRemoveGroup = false;
            }
        );
    }

    select() {
        this.forceRemoveGroup = !this.forceRemoveGroup;
    }

    cancel() {
        this.dialogRef.close();
    }
}
