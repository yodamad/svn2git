/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationHistoryDetailComponent } from 'app/entities/migration-history/migration-history-detail.component';
import { MigrationHistory } from 'app/shared/model/migration-history.model';

describe('Component Tests', () => {
    describe('MigrationHistory Management Detail Component', () => {
        let comp: MigrationHistoryDetailComponent;
        let fixture: ComponentFixture<MigrationHistoryDetailComponent>;
        const route = ({ data: of({ migrationHistory: new MigrationHistory(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationHistoryDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(MigrationHistoryDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MigrationHistoryDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.migrationHistory).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
