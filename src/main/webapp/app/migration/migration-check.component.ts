import { FormBuilder, FormGroup } from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { IMigration, Migration, StatusEnum } from 'app/shared/model/migration.model';
import { HttpResponse } from '@angular/common/http';
import { JhiParseLinks } from 'ng-jhipster';
import { MatDialog, MatSnackBar, MatSnackBarConfig } from '@angular/material';
import { TranslateService } from '@ngx-translate/core';
import { MigrationService } from 'app/entities/migration';
import { JhiConfirmRetryModalComponent } from 'app/migration/confirm-retry.component';
import { ActivatedRoute, Router } from '@angular/router';
import { MigrationFilter } from 'app/shared/model/migration-filter.model';

/**
 * Page to check migrations status
 */
@Component({
    selector: 'jhi-migration-check.component',
    templateUrl: 'migration-check.component.html',
    styleUrls: ['migration-check.component.css']
})
export class MigrationCheckComponent implements OnInit {
    searchFormGroup: FormGroup;
    migrations: IMigration[] = [];

    // SnackBar config
    snackBarConfig = new MatSnackBarConfig();

    // Pagination config
    pageSizeOptions: number[];
    length: number;

    constructor(
        private _formBuilder: FormBuilder,
        private _migrationProcessService: MigrationProcessService,
        private _migrationService: MigrationService,
        private parseLinks: JhiParseLinks,
        private _errorSnackBar: MatSnackBar,
        private _translationService: TranslateService,
        private _matDialog: MatDialog,
        private _router: Router,
        private _route: ActivatedRoute
    ) {
        // Init snack bar configuration
        this.snackBarConfig.duration = 5000;
        this.snackBarConfig.verticalPosition = 'top';
        this.snackBarConfig.horizontalPosition = 'center';
    }

    /**
     * On init
     */
    ngOnInit(): void {
        this.load(this._route.snapshot.queryParams);
    }

    /**
     * Load check page
     * @param queryParams the query params
     */
    load(queryParams?): void {
        this.searchFormGroup = this._formBuilder.group({
            pageIndex: [queryParams && queryParams['pageIndex'] ? queryParams['pageIndex'] : 0],
            pageSize: [queryParams && queryParams['pageSize'] ? queryParams['pageSize'] : 5],
            user: [queryParams && queryParams['user'] ? queryParams['user'] : ''],
            group: [queryParams && queryParams['group'] ? queryParams['group'] : ''],
            project: [queryParams && queryParams['project'] ? queryParams['project'] : '']
        });
        this.page(null);
        if (!queryParams) {
            this._router.navigate(['/migration-check']);
        }
    }

    /**
     * If the reset button must be visible
     */
    showResetButton(): boolean {
        return (
            this.searchFormGroup &&
            (+this.searchFormGroup.value.pageIndex !== 0 ||
                +this.searchFormGroup.value.pageSize !== 5 ||
                this.searchFormGroup.value.user !== '' ||
                this.searchFormGroup.value.group !== '' ||
                this.searchFormGroup.value.project !== '')
        );
    }

    /**
     * Retry a migration
     */
    retry(id: number): void {
        const dialog = this._matDialog.open(JhiConfirmRetryModalComponent, {
            data: { migId: id }
        });
        dialog.afterClosed().subscribe((migrationId: number) => this._router.navigate(['/migration/' + migrationId + '/view']));
    }

    /**
     * Add class according to status
     * @param status
     */
    cssClass(status: StatusEnum): string {
        if (status === StatusEnum.DONE || status === StatusEnum.DONE_WITH_WARNINGS) {
            return 'badge-success';
        }
        if (status === StatusEnum.FAILED) {
            return 'badge-danger';
        }
        if (status === StatusEnum.RUNNING) {
            return 'badge-primary';
        }
        if (status === StatusEnum.WAITING) {
            return 'cell-waiting';
        }
    }

    /**
     * Open snack bar to display error message
     * @param errorCode
     * @param error
     */
    openSnackBar(errorCode: string, error = true): void {
        if (error) {
            this.snackBarConfig.panelClass = ['errorPanel'];
        } else {
            this.snackBarConfig.panelClass = ['warnPanel'];
        }
        this._errorSnackBar.open(this._translationService.instant(errorCode), null, this.snackBarConfig);
    }

    /**
     * Load the current page
     * @param event the change page event
     */
    page(event?): void {
        this._migrationProcessService.findMigrations(this.getFilter(event)).subscribe(
            (res: HttpResponse<IMigration[]>) => {
                this.migrations = res.body;
                this.length = parseInt(res.headers.get('X-Total-Count'), 10);
                // If pagination unavailable
                if (!this.length) {
                    this.length = this.migrations.length;
                    this.searchFormGroup.controls['pageSize'].setValue(this.length);
                    this.pageSizeOptions = null;
                } else {
                    this.pageSizeOptions = [];
                    if (this.length > 5) {
                        this.pageSizeOptions.push(5);
                    }
                    if (this.length > 25) {
                        this.pageSizeOptions.push(25);
                    }
                    if (this.length > 100) {
                        this.pageSizeOptions.push(100);
                    }
                    this.pageSizeOptions.push(this.length);
                }
            },
            () => this.openSnackBar('error.http.504')
        );
    }

    /**
     * Get the filter
     */
    getFilter(event): MigrationFilter {
        if (event !== null) {
            this.searchFormGroup.controls['pageIndex'].setValue(event ? event.pageIndex : 0);
            this.searchFormGroup.controls['pageSize'].setValue(event ? event.pageSize : 5);
            this._router.navigate([], {
                queryParams: this.searchFormGroup.value,
                queryParamsHandling: 'merge'
            });
        }
        return this.searchFormGroup.value;
    }

    /**
     * Export migrations on csv file
     */
    exportToCsv() {
        let csv = '';
        const separator = ';';

        // Select cols
        const keys = [
            'id',
            'date',
            'createdTimestamp',
            'user',
            'svnUrl',
            'svnUser',
            'svnGroup',
            'svnProject',
            'svnHistory',
            'maxFileSize',
            'forbiddenFileExtensions',
            'trunk',
            'tagsToMigrate',
            'branchesToMigrate',
            'gitlabUrl',
            'gitlabGroup',
            'gitlabProject',
            'status'
        ];

        // Head
        let head = '';
        keys.forEach((key: string, i: number) => {
            head += key + (i !== keys.length - 1 ? separator : '');
        });
        csv += head + '\r\n';

        // Rows
        this.migrations.forEach((migration: IMigration, i: number) => {
            let line = '';
            keys.forEach((key: string, j: number) => {
                let value = this.migrations[i][key];
                if (value === null || value === undefined) {
                    value = '';
                }
                if (key === 'createdTimestamp') {
                    const date = new Date(value);
                    if (date.toString() !== 'Invalid Date') {
                        value = date.toLocaleString('en-GB', { hour: 'numeric', minute: 'numeric', second: 'numeric' });
                    }
                } else {
                    value = value
                        .toString()
                        .split(separator)
                        .join(',');
                }
                line += (j ? separator : '') + value;
            });
            csv += line + '\r\n';
        });

        // Export
        const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
        const a = document.createElement('a');
        const href = URL.createObjectURL(blob);
        a.setAttribute('target', '_blank');
        a.setAttribute('href', href);
        a.setAttribute('download', 'migrations.csv');
        a.style.visibility = 'hidden';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }
}
