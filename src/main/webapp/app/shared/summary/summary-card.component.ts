import { Component, Input, OnInit } from '@angular/core';
import { IMigration } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration';
import { MatDialog } from '@angular/material';
import { JhiRemovedFilesModalComponent } from 'app/migration/removed-files.component';

/**
 * Migration summary component
 */
@Component({
    selector: 'jhi-summary-card',
    templateUrl: 'summary-card.component.html',
    styleUrls: ['summary-card.component.css']
})
export class SummaryCardComponent implements OnInit {
    @Input() migration: IMigration;
    nbRemovedFiles: number;

    constructor(private _migrationService: MigrationService, private _matDialog: MatDialog) {}

    ngOnInit(): void {
        this._migrationService.countRemovedFiles(this.migration.id).subscribe(res => (this.nbRemovedFiles = res.body));
    }

    /**
     * Only display max size if set (first character is a digit)
     */
    displayMaxSize() {
        return this.migration.maxFileSize !== undefined && this.migration.maxFileSize.match(/^\d/);
    }

    displayDetails() {
        this._matDialog.open(JhiRemovedFilesModalComponent, {
            data: { migrationId: this.migration.id }
        });
    }
}
