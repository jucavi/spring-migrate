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
