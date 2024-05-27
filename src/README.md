# Modificaciones
## filter-local-properties
```editorconfig
filter.rootPath=c:/
```

## FileController#updateFile
- Descomentado endpoint update
- modificado #isvalid()
```java
dto.isValid(false, true);
```
- Añadimos 
```java
parentDirectory = (Directory) Hibernate.unproxy(parentDirectory);
fileType = (FileType) Hibernate.unproxy(fileType);
```
-Se agrega try-catch
* linea 622
```java
// Added try catch for debug
try {
    // only move physical
    entity = fileService.updateFile(FileDto.convertToEntity(dto, parentDirectory,
            fileType));
} catch (Exception ex) {
    // Set entity null
    entity = null;
}
```

## FileServiceImpl#updateFile
* linea 90
```java
} else {

    // moveFile(getFilePathBase(oldFile), getFilePathBase(updatedFile), oldFile.getName(), updatedFile.getName());

}
```


## DirectoryController#getAll
- Sustituimos por error 500
```java
// COMENTTADO POR ERROR 500: NuSuchElementException
//		List<DirectoryDto> dtos = entities.stream().map(directory -> {
//			DirectoryDto dto = DirectoryDto.convertToDto(directory);
//			dto.setPathBase(directoryService.getDirectoryPathBase(directory));
//			return dto;
//		}).collect(Collectors.toList());
```
a
```java
List<DirectoryDto> dtos = new ArrayList<>();
for (Directory directory : entities) {
    try {
       DirectoryDto dto = DirectoryDto.convertToDto(directory);
       String d = directoryService.getDirectoryPathBase(directory);
       dto.setPathBase(d);
       dtos.add(dto);
    } catch (Exception ex) {
       System.out.println(ex.getMessage());
    }
}
 
Hibernate.unproxy(dtos);
```

* Un borrado con purga borra el directorio físico, así que comentamos la linea 391
```java
// deletePhisicalDirectory(directory);
```

## DirectoryServiceImp#updateDirectory
- Se comenta porque fisicamente no existen los directorios
  1. Si los directorios existen se puede dejar comentado
```java
		// Se mueve el directorio
		if (Boolean.parseBoolean(env.getProperty(PROPERTY_UUID_REPRESENTATION))) {

//			moveDirectory(getDirectoryPathBase(oldDirectory), getDirectoryPathBase(updatedDirectory),
//					oldDirectory.getId().toString(), updatedDirectory.getId().toString());

		} else {

//			moveDirectory(getDirectoryPathBase(oldDirectory), getDirectoryPathBase(updatedDirectory),
//					oldDirectory.getName(), updatedDirectory.getName());
```


## RootDirectoryController
- Agrego toda la clase porque estas modificaciones no quedaron documentadas, diría que solo se añadió #truncate
```java
package es.snc.document.manager.api.controller;

import es.snc.common.persistence.enumeration.ApiError;
import es.snc.common.util.ApiErrorUtil;
import es.snc.common.util.JavaUtil;
import es.snc.document.manager.api.controller.DirectoryController;
import es.snc.document.manager.business.service.IDirectoryService;
import es.snc.document.manager.business.service.IRootDirectoryService;
import es.snc.document.manager.dto.DirectoryDto;
import es.snc.document.manager.dto.ResourceDto;
import es.snc.document.manager.persistence.model.Directory;
import es.snc.document.manager.persistence.model.RootDirectory;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 */
@RestController
@RequestMapping(value = "roots")
public class RootDirectoryController {

    private final IRootDirectoryService rootDirectoryService;

    private static final String PROPERTY_UUID_REPRESENTATION = "uuid.representation.enabled";

    private final Logger LOGGER = LoggerFactory.getLogger(DirectoryController.class);

    @Autowired
    public RootDirectoryController(IRootDirectoryService rootDirectoryService) {
        this.rootDirectoryService = rootDirectoryService;

    }

    @GetMapping()
    public ResponseEntity<List<RootDirectory>> findAll() {

        List<RootDirectory> entities = rootDirectoryService.getAll();
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/filterBy/{directoryId}")
    public ResponseEntity<List<RootDirectory>> findByDirectoryId(@PathVariable UUID directoryId) {

        ResponseEntity<List<RootDirectory>> response;

        List<RootDirectory> entities = rootDirectoryService.getAllByDirectoryId(directoryId);

        Hibernate.unproxy(entities);

        response = ResponseEntity.ok(JavaUtil.emptyIfNull(entities));

        return response;
    }


    @DeleteMapping("/directory/{directoryId}")
    public ResponseEntity<Void> deleteByDirectoryId(@PathVariable UUID directoryId) {

        ResponseEntity<Void> response;

        try {
            rootDirectoryService.deleteByDirectoryId(directoryId);
            response = ResponseEntity.ok().build();
        } catch (Exception ex) {
            //
            response = (ResponseEntity<Void>) ApiErrorUtil.buildErrorResponse(ApiError.CONFLICT_CANNOT_DELETE,ex.getMessage());
        }

        return response;
    }

    @DeleteMapping("/truncate")
    public ResponseEntity<Void> truncate() {

        ResponseEntity<Void> response;
        try {
            rootDirectoryService.truncateTable();
            response = ResponseEntity.ok().build();
        } catch (SQLException e) {
            response = (ResponseEntity<Void>) ApiErrorUtil.buildErrorResponse(ApiError.CONFLICT_CANNOT_DELETE,
                    e.getMessage());
        }

        return response;
    }


    @PostMapping()
    public ResponseEntity<RootDirectory> createRootDirectory(@RequestBody RootDirectory rootDirectory) {
        RootDirectory response = rootDirectoryService.createRoot(rootDirectory);
        return ResponseEntity.ok(response);
    }
}
```

## IRootDirectoryDao
- Se añade truncateTable
```java
public interface IRootDirectoryDao 
extends IGenericDaoWithFilter<RootDirectory, Long, RootDirectoryFilter>{

    // Added by: Juan Carlos
    List<RootDirectory> findByDirectoryId(UUID directoryId);
    // Added by: Daniel
    void truncateTable();
    void deleteByDirectoryId(UUID directoryId);
}
```

## IRootDirectoryDaoImpl
- Se añade truncateTable, los métodos restantes se utilizaban al principio pero finalmente no se usan
```java
@Override
	@Transactional
	public List<RootDirectory> findByDirectoryId(UUID directoryId) {

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<RootDirectory> criteriaQuery = criteriaBuilder.createQuery(RootDirectory.class);
		Root<RootDirectory> rootDirectory = criteriaQuery.from(RootDirectory.class);

		Directory dir = new Directory();
		dir.setId(directoryId);

		Predicate predicate = criteriaBuilder.equal(rootDirectory.get(PROPERTY_DIRECTORY), dir);

		criteriaQuery.where(predicate);


		try {
			return entityManager.createQuery(criteriaQuery).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		}
	}

	/**
	 * @author Daniel Espinosa
	 */
	@Override
	@Transactional
	public void truncateTable() {
		String sql = "TRUNCATE TABLE snc_document_manager_root_directory";
		Query query = entityManager.createNativeQuery(sql);
		query.executeUpdate();
	}

	@Override
	@Transactional
	public void deleteByDirectoryId(UUID directoryId) {

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaDelete<RootDirectory> query = criteriaBuilder.createCriteriaDelete(RootDirectory.class);
		Root<RootDirectory> rootDirectory = query.from(RootDirectory.class);

		Directory child = new Directory();
		child.setId(directoryId);

		query.where(criteriaBuilder.equal(rootDirectory.get(PROPERTY_DIRECTORY), child));

		try {
			entityManager.createQuery(query).executeUpdate();
		} catch (NoResultException e) {
			//
		}
	}
```

## IRootDirectoryService
- Se añade truncateTable
```java
public interface IRootDirectoryService 
extends IFilterService<RootDirectory, Long, RootDirectoryFilter>  {

    List<RootDirectory> findAll();
    List<RootDirectory> getAllByDirectoryId(UUID directoryId);
    void truncateTable() throws SQLException;
    void deleteByDirectoryId(UUID directoryId);
    RootDirectory createRoot(RootDirectory root);
}
```

