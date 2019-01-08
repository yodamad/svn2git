import { FormBuilder, FormGroup } from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { IMigration, StatusEnum } from 'app/shared/model/migration.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { JhiParseLinks } from 'ng-jhipster';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material';
import { TranslateService } from '@ngx-translate/core';

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
    links: any;
    totalItems: number;

    // SnackBar config
    snackBarConfig = new MatSnackBarConfig();

    constructor(
        private _formBuilder: FormBuilder,
        private _migrationService: MigrationProcessService,
        private parseLinks: JhiParseLinks,
        private _errorSnackBar: MatSnackBar,
        private _translationService: TranslateService
    ) {
        // Init snack bar configuration
        this.snackBarConfig.panelClass = ['errorPanel'];
        this.snackBarConfig.duration = 5000;
        this.snackBarConfig.verticalPosition = 'top';
        this.snackBarConfig.horizontalPosition = 'center';
    }

    ngOnInit() {
        this.searchFormGroup = this._formBuilder.group({
            userCriteria: [''],
            groupCriteria: ['']
        });
        this.links = {
            last: 0
        };
    }

    /**
     * Search migration according to criteria
     */
    search() {
        this.migrations = [];
        if (this.searchFormGroup.controls['userCriteria'].value !== '') {
            this._migrationService
                .findMigrationByUser(this.searchFormGroup.controls['userCriteria'].value)
                .subscribe(
                    (res: HttpResponse<IMigration[]>) => this.paginateMigrations(res.body, res.headers),
                    () => this.openSnackBar('error.http.504')
                );
        } else if (this.searchFormGroup.controls['groupCriteria'].value !== '') {
            this._migrationService
                .findMigrationByGroup(this.searchFormGroup.controls['groupCriteria'].value)
                .subscribe(
                    (res: HttpResponse<IMigration[]>) => this.paginateMigrations(res.body, res.headers),
                    () => this.openSnackBar('error.http.504')
                );
        } else {
            alert('Enter a criteria');
        }
    }

    private paginateMigrations(data: IMigration[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        for (let i = 0; i < data.length; i++) {
            this.migrations.push(data[i]);
        }
        console.log(this.migrations);
    }

    /**
     * Add class according to status
     * @param status
     */
    cssClass(status: StatusEnum) {
        if (status === StatusEnum.DONE) {
            return 'badge-success';
        }
        if (status === StatusEnum.FAILED) {
            return 'badge-danger';
        }
        if (status === StatusEnum.RUNNING) {
            return 'badge-primary';
        }
    }

    /**
     * Open snack bar to display error message
     * @param errorCode
     */
    openSnackBar(errorCode: string) {
        this._errorSnackBar.open(this._translationService.instant(errorCode), null, this.snackBarConfig);
    }
}
