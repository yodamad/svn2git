import { FormGroup, FormBuilder } from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { IMigration } from 'app/shared/model/migration.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { JhiParseLinks } from 'ng-jhipster';

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

    constructor(private _formBuilder: FormBuilder, private _migrationService: MigrationProcessService, private parseLinks: JhiParseLinks) {}

    ngOnInit() {
        this.searchFormGroup = this._formBuilder.group({
            userCriteria: [''],
            groupCriteria: ['']
        });
        this.links = {
            last: 0
        };
    }

    search() {
        this.migrations = [];
        if (this.searchFormGroup.controls['userCriteria'].value !== '') {
            this._migrationService
                .findMigrationByUser(this.searchFormGroup.controls['userCriteria'].value)
                .subscribe((res: HttpResponse<IMigration[]>) => this.paginateMigrations(res.body, res.headers));
        } else if (this.searchFormGroup.controls['groupCriteria'].value !== '') {
            this._migrationService
                .findMigrationByGroup(this.searchFormGroup.controls['groupCriteria'].value)
                .subscribe((res: HttpResponse<IMigration[]>) => this.paginateMigrations(res.body, res.headers));
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
}
