/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationRemovedFileDetailComponent } from 'app/entities/migration-removed-file/migration-removed-file-detail.component';
import { MigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';

describe('Component Tests', () => {
    describe('MigrationRemovedFile Management Detail Component', () => {
        let comp: MigrationRemovedFileDetailComponent;
        let fixture: ComponentFixture<MigrationRemovedFileDetailComponent>;
        const route = ({ data: of({ migrationRemovedFile: new MigrationRemovedFile(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationRemovedFileDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(MigrationRemovedFileDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MigrationRemovedFileDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.migrationRemovedFile).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
