/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationRemovedFileComponent } from 'app/entities/migration-removed-file/migration-removed-file.component';
import { MigrationRemovedFileService } from 'app/entities/migration-removed-file/migration-removed-file.service';
import { MigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';

describe('Component Tests', () => {
    describe('MigrationRemovedFile Management Component', () => {
        let comp: MigrationRemovedFileComponent;
        let fixture: ComponentFixture<MigrationRemovedFileComponent>;
        let service: MigrationRemovedFileService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationRemovedFileComponent],
                providers: []
            })
                .overrideTemplate(MigrationRemovedFileComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(MigrationRemovedFileComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationRemovedFileService);
        });

        it('Should call load all on init', () => {
            // GIVEN
            const headers = new HttpHeaders().append('link', 'link;link');
            spyOn(service, 'query').and.returnValue(
                of(
                    new HttpResponse({
                        body: [new MigrationRemovedFile(123)],
                        headers
                    })
                )
            );

            // WHEN
            comp.ngOnInit();

            // THEN
            expect(service.query).toHaveBeenCalled();
            expect(comp.migrationRemovedFiles[0]).toEqual(jasmine.objectContaining({ id: 123 }));
        });
    });
});
