# hotel-hexagonal

Proyecto integrador del curso **Desarrollo Orientado a Servicios 2026-2** (Universidad Santo Tomás - Tunja).
Es la migración del proyecto **hotel_A** (Spring Boot + PostgreSQL + Thymeleaf) a **arquitectura hexagonal**
con **Java 21 + Quarkus**, **MySQL (XAMPP)**, **Qute + Bootstrap 5** y **reportes PDF con OpenPDF**,
siguiendo la misma estructura del proyecto **cine-hexagonal** del curso.

> **Convenciones:** identificadores (clases, atributos, métodos) en **inglés**; mensajes al usuario y
> comentarios en **español**.

---

## 1. La problemática (propuesta en tres frases)

- **Problema:** el Hotel Westin registra las reservas a mano y se le cruzan las fechas de una misma habitación,
  no sabe qué habitaciones están ocupadas o en mantenimiento y calcula mal el valor de las estadías.
- **A quién le sirve:** al personal de recepción, que registra huéspedes, crea reservas y hace el check-in / check-out,
  y a la administración, que consulta los reportes de ocupación e ingresos.
- **Qué datos guarda y consulta:** habitaciones (tipo, precio, capacidad, estado), huéspedes (documento y contacto)
  y reservas (fechas, personas, total y estado), que se crean, se listan y cambian de estado.

---

## 2. Arquitectura hexagonal (dónde está cada cosa)

```
hotel-hexagonal/
|-- pom.xml                     Dependencias (Quarkus, Hibernate ORM Panache, MySQL, Qute, OpenAPI, OpenPDF, Lombok)
|-- mvnw / mvnw.cmd             Maven Wrapper (no hace falta instalar Maven)
|-- .run/                       Configuración de ejecución para IntelliJ IDEA ("Hotel - quarkus dev")
|-- src/main/java/usta/hotel/
|   |-- domain/                 NÚCLEO: reglas del negocio. Sin @Entity, sin Quarkus, sin vista.
|   |   |-- model/              Room, RoomStatus, Guest, Reservation, ReservationStatus
|   |   |-- service/            BookingService, PriceCalculator, RoomPolicy, GuestPolicy
|   |   |-- exception/          BusinessRuleException
|   |-- application/            Casos de uso + puertos
|   |   |-- port/in/            Puertos de ENTRADA: ManageRoomsUseCase, ManageGuestsUseCase, ManageReservationsUseCase
|   |   |-- port/out/           Puertos de SALIDA: RoomRepository, GuestRepository, ReservationRepository
|   |   |-- usecase/            ManageRoomsService, ManageGuestsService, ManageReservationsService
|   |   |-- exception/          ResourceNotFoundException
|   |-- infrastructure/
|       |-- adapter/in/web/     ADAPTADOR WEB (única entrada): recursos Qute, reportes PDF, mapper de la vista
|       |-- adapter/out/persistence/   ADAPTADOR JPA (MySQL)
|       |   |-- entity/         RoomEntity, GuestEntity, ReservationEntity (@Entity, @Table, @ManyToOne)
|       |   |-- repository/     Repositorios Panache
|       |   |-- mapper/         Conversión dominio <-> entidad
|       |   |-- adapter/        JpaRoomRepository, JpaGuestRepository, JpaReservationRepository (implementan los puertos)
|       |-- config/             DataSeeder: datos de prueba al arrancar
|-- src/main/resources/
    |-- application.properties  MySQL, Hibernate, Swagger
    |-- templates/              Vistas Qute (layout, index, room-form, guests, reservations, reservation-form, reservation-detail, reports, error)
    |-- META-INF/resources/     js/theme.js (modo claro/oscuro), img/rooms/ (foto por tipo), img/hotel/ (portada)
```

**Regla de dependencias:** `infrastructure` -> `application` -> `domain`. El dominio no importa nada de
Jakarta, Quarkus ni de la vista (solo Lombok para getters/setters, igual que en el cine).
**No hay adaptador REST ni de consola:** la única entrada es la vista HTML.

### ¿Qué busco? -> ¿Dónde está?

| Quiero ver... | Archivo |
|---|---|
| Reglas de las reservas (cruces, capacidad, fechas, estados) | `domain/service/BookingService.java` |
| Cálculo del valor de la estadía | `domain/service/PriceCalculator.java` |
| Reglas de habitaciones y huéspedes | `domain/service/RoomPolicy.java`, `GuestPolicy.java` |
| Casos de uso | `application/usecase/*Service.java` |
| Puertos de entrada / salida | `application/port/in/`, `application/port/out/` |
| Tablas y relaciones (`@ManyToOne`, `@JoinColumn`) | `infrastructure/adapter/out/persistence/entity/ReservationEntity.java` |
| Mappers dominio <-> entidad | `infrastructure/adapter/out/persistence/mapper/` |
| Adaptador web (páginas) | `infrastructure/adapter/in/web/RoomWebResource.java`, `GuestWebResource.java`, `ReservationWebResource.java` |
| Reportes PDF | `infrastructure/adapter/in/web/ReportResource.java` y `PdfReports.java` |
| Páginas de error (404/400) | `infrastructure/adapter/in/web/WebExceptionMappers.java` + `templates/error.html` |
| Datos de prueba | `infrastructure/config/DataSeeder.java` |
| Modo claro / oscuro | `META-INF/resources/js/theme.js` + botón en `templates/layout.html` |

---

## 3. Base de datos

- **Motor:** MySQL / MariaDB de **XAMPP** (puerto 3306, usuario `root`, sin contraseña).
- **Nombre de la base:** **`hotel_hexagonal`** (se crea sola con `createDatabaseIfNotExist=true`).
- **Tablas** (se crean/actualizan solas con `quarkus.hibernate-orm.schema-management.strategy=update`):

| Tabla | Columnas principales | Relación |
|---|---|---|
| `rooms` | id, room_number (único), room_type, description, price_per_night, capacity, status | - |
| `guests` | id, document (único), first_name, last_name, email (único), phone, city, registered_at | - |
| `reservations` | id, guest_id, room_id, check_in, check_out, guests_count, total, status, created_at | **@ManyToOne** con `guests` (guest_id) y con `rooms` (room_id) |

- **Datos de prueba:** la primera vez que arranca (tablas vacías) `DataSeeder` carga 7 habitaciones
  (una en mantenimiento), 5 huéspedes y 8 reservas en distintos estados (hospedado, confirmadas,
  finalizadas y una cancelada), con fechas relativas al día de hoy para que la aplicación se vea funcionando.
  Se puede desactivar con `hotel.seed.enabled=false`.

---

## 4. Reglas de negocio (dominio)

**Reservas (`BookingService`):**
- La fecha de entrada no puede ser anterior a hoy y la salida debe ser posterior a la entrada.
- Estadía máxima de **30 noches**.
- No se puede reservar una habitación **en mantenimiento**.
- La cantidad de huéspedes debe estar entre 1 y la **capacidad** de la habitación.
- **Sin cruces:** una habitación no puede tener dos reservas activas (confirmada u hospedado) que se crucen.
- **Estados:** `CONFIRMED` -> `CHECKED_IN` (check-in, solo desde la fecha de entrada) -> `COMPLETED` (check-out).
  Solo se cancela (`CANCELLED`) una reserva confirmada.

**Precio (`PriceCalculator`):** precio por noche + **15%** las noches de viernes y sábado; **10% de descuento**
en estadías de 7 noches o más.

**Habitaciones (`RoomPolicy`):** número de 2 a 4 dígitos y único, tipo Sencilla, Doble, Familiar, Suite Dúplex o Suite
Panorámica, precio > 0 y capacidad coherente con el tipo (Sencilla 1, Doble 2, Suite Panorámica 2, Suite Dúplex 4,
Familiar 6); no pasa a mantenimiento si tiene
un huésped hospedado.

**Huéspedes (`GuestPolicy`):** documento de 5 a 15 dígitos y único, correo válido y único, teléfono de 7 a 15 dígitos.

Los errores se muestran en la página con alertas de Bootstrap (`alert-success` / `alert-danger`).

---

## 5. Pantallas (todas desde el navegador)

| Ruta | Pantalla |
|---|---|
| `/` | **Principal:** resumen del día y listado de habitaciones (reservar, editar, mantenimiento) |
| `/rooms/new`, `/rooms/{id}/edit` | Formulario de habitación (crear / actualizar) |
| `/guests` | Listado y formulario de registro de huéspedes |
| `/reservations` | Listado de reservas con filtro por estado y acciones (check-in, check-out, cancelar) |
| `/reservations/new` | **Formulario de reserva** |
| `/reservations/{id}` | **Detalle** de la reserva con el desglose de la cuenta |
| `/reports` | **Pantalla de reportes:** resumen por rango de fechas + descargas PDF |

Todas las páginas tienen `navbar`, `main` y `footer`, y un botón para cambiar entre **modo claro y oscuro**
(Bootstrap 5.3 `data-bs-theme`; la preferencia se recuerda en el navegador). La apariencia se hace **solo con
clases de Bootstrap** (no hay archivo CSS propio).

**Fotos de las habitaciones:** la imagen la define el tipo, para que siempre corresponda a la habitación:
`img/rooms/sencilla.jpg`, `doble.jpg`, `familiar.jpg`, `suite-duplex.jpg` y `suite-panoramica.jpg`. Si falta alguna, la vista muestra un ícono del tipo. Para cambiar una foto
basta con reemplazar el archivo con el mismo nombre.

### Reportes PDF (OpenPDF)
- `/reports/rooms.pdf` - listado de habitaciones con su estado del día.
- `/reports/reservations.pdf?from=2026-11-01&to=2026-11-30` - reservas por rango de fechas con total de ingresos.
- `/reports/receipt/{id}.pdf` - comprobante de una reserva.

### Swagger
- **Guía paso a paso con todas las pruebas:** [`PRUEBAS_SWAGGER.md`](PRUEBAS_SWAGGER.md).
- Interfaz: **http://localhost:8080/q/swagger-ui**
- Documento OpenAPI: **http://localhost:8080/q/openapi**
- Cada recurso de la vista y de reportes lleva `@Tag` y `@Operation`. La configuración (título, versión,
  descripción) está en `application.properties` (`quarkus.smallrye-openapi.*`).

---

## 6. Requisitos

| Herramienta | Versión |
|---|---|
| JDK | **21** (Temurin, Oracle u otro) |
| IntelliJ IDEA | Community o Ultimate (reciente) |
| XAMPP | Con **MySQL** encendido en el puerto 3306 |
| Maven | No hace falta: se usa el wrapper `mvnw` incluido |

**Plugins de IntelliJ:** **Lombok** (obligatorio; en versiones nuevas ya viene incluido) y **Quarkus Tools**
(opcional, recomendado).

---

## 7. Cómo ejecutar (paso a paso)

### Paso 1. Encender MySQL en XAMPP
1. Abra el **XAMPP Control Panel**.
2. Pulse **Start** en **MySQL** (y en **Apache** si quiere usar phpMyAdmin).
3. No necesita crear la base: `hotel_hexagonal` se crea sola al arrancar.

> Si su MySQL tiene contraseña para `root`, póngala en `src/main/resources/application.properties`
> en `quarkus.datasource.password=`.

### Paso 2. Abrir el proyecto en IntelliJ IDEA
1. Descomprima el zip (por ejemplo en `C:\proyectos\hotel-hexagonal`).
2. En IntelliJ: **File > Open...** y seleccione la carpeta `hotel-hexagonal` (la que tiene el `pom.xml`).
   Si pregunta, elija **Open as Project** y **Trust Project**.
3. Configure el JDK: **File > Project Structure > Project > SDK = 21** (si no lo tiene: *Add SDK > Download JDK*, versión 21).
4. Active Lombok: **File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors >
   Enable annotation processing** (marcado).

### Paso 3. Descargar las dependencias
IntelliJ las descarga solo al abrir el proyecto (barra inferior: *Resolving dependencies...*). Si no lo hace:
- Abra la pestaña **Maven** (a la derecha) y pulse **Reload All Maven Projects** (icono de flechas), o
- En la terminal de IntelliJ ejecute:

```powershell
.\mvnw.cmd dependency:resolve        # Windows
./mvnw dependency:resolve            # Linux / macOS
```

La primera vez tarda unos minutos (descarga Quarkus, Hibernate, el driver de MySQL, OpenPDF, etc.).

### Paso 4. Ejecutar

Opción A: en la pestaña **Maven** > *Plugins* > *quarkus* > doble clic en **quarkus:dev**.

Opción B: en la terminal:

```powershell
.\mvnw.cmd quarkus:dev               # Windows
./mvnw quarkus:dev                   # Linux / macOS
```

Cuando en la consola aparezca `Listening on: http://localhost:8080`, abra:

- Aplicación: **http://localhost:8080/**
- Swagger: **http://localhost:8080/q/swagger-ui**
- Reportes: **http://localhost:8080/reports**

Para detener: `Ctrl + C` en la consola (o el botón ■ de IntelliJ).

### Paso 5. Evidencia en phpMyAdmin
1. Abra **http://localhost/phpmyadmin**.
2. Seleccione la base **hotel_hexagonal**: verá las tablas `rooms`, `guests` y `reservations` con sus datos.
3. En `reservations` > pestaña **Estructura** > **Vista de relaciones** se ven las llaves foráneas
   `guest_id -> guests.id` y `room_id -> rooms.id`.

### Empaquetar (opcional)

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\quarkus-app\quarkus-run.jar
```

---

## 8. Recorrido sugerido para la sustentación

1. **Principal (`/`):** resumen del día y habitaciones; la 203 aparece *En mantenimiento* y la 102 *Ocupada hoy*.
2. **Crear:** registrar un huésped en `/guests` y una reserva en `/reservations/new`.
3. **Reglas:** intentar reservar la 102 para hoy (cruce de fechas), poner 3 personas en la 101 (capacidad)
   o poner en mantenimiento la 102 (tiene un huésped hospedado): el error aparece en rojo en la página.
   La 203 está en mantenimiento, así que su botón *Reservar* está deshabilitado y no aparece en el formulario.
4. **Cambiar estado:** en `/reservations` hacer **check-in** a la reserva de Sofía (entra hoy), luego **check-out**;
   cancelar una reserva confirmada.
5. **PDF:** descargar el comprobante desde el detalle y el reporte por rango desde `/reports`.
6. **Swagger:** mostrar `/q/swagger-ui` con los recursos agrupados por `@Tag`.
7. **phpMyAdmin:** mostrar tablas, llaves foráneas y datos.
8. **Modo oscuro:** botón *Modo oscuro / Modo claro* en la barra superior.

---

## 9. Problemas comunes

| Problema | Solución |
|---|---|
| `Communications link failure` / `Connection refused` | MySQL de XAMPP está apagado: enciéndalo en el panel. |
| `Access denied for user 'root'` | Ponga la contraseña de su MySQL en `quarkus.datasource.password`. |
| `Port 8080 already in use` | Cierre lo que use ese puerto o cambie `quarkus.http.port` en `application.properties`. |
| Errores con `getX()` / `builder()` en IntelliJ | Lombok no está activo: active *Enable annotation processing* y reinicie IntelliJ. |
| `release version 21 not supported` | El proyecto no usa JDK 21: cámbielo en *Project Structure* y en *Settings > Build Tools > Maven > Runner > JRE*. |
| Quiero volver a cargar los datos de prueba (o aplicar cambios en los datos de ejemplo) | En phpMyAdmin elimine la base `hotel_hexagonal` y vuelva a ejecutar. |
| Las páginas salen sin estilos | Bootstrap se carga por CDN: necesita conexión a Internet. |

---


