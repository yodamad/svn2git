import { Component, Input, OnInit } from '@angular/core';
import { IMigration } from 'app/shared/model/migration.model';

/**
 * Migration summary component
 */
@Component({
    selector: 'jhi-summary-card',
    templateUrl: 'summary-card.component.html',
    styleUrls: ['summary-card.component.css']
})
export class SummaryCardComponent {
    @Input() migration: IMigration;

    /**
     * Only display max size if set (first character is a digit)
     */
    displayMaxSize() {
        return this.migration.maxFileSize !== undefined && this.migration.maxFileSize.match(/^\d/);
    }
}
