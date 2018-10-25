/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationDetailComponent } from 'app/entities/migration/migration-detail.component';
import { Migration } from 'app/shared/model/migration.model';

describe('Component Tests', () => {
    describe('Migration Management Detail Component', () => {
        let comp: MigrationDetailComponent;
        let fixture: ComponentFixture<MigrationDetailComponent>;
        const route = ({ data: of({ migration: new Migration(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(MigrationDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MigrationDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.migration).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
