# Payment Links API 💳

## Descripción 
API REST para gestión de enlaces de pago con autenticación JWT, procesamiento de pagos simulados y manejo de estados (CREATED, PAID, EXPIRED).

## Tecnologías utilizadas 
- **Backend**: Java 17, Spring Boot 3.x  
- **Base de Datos**: PostgreSQL  
- **Seguridad**: JWT, Spring Security  
- **Herramientas**: MapStruct, Lombok, Hibernate  

## Configuración e instalación 

### 1. Clona el repositorio.

```sh
git clone https://github.com/laura2ndrea/payment-links.git
```
### 2. Abre el proyecto en la IDE de tu preferencia.

### 3. Configura la base de datos en application.properties

### 4. Corre la clase principal de la aplicación PaymentLinksApplication

Importante: La aplicación actualmente tiene problemas de compilación en el mapeo entre entidades y DTOs usando MapStruct. Por lo tanto por ahora no corre. 

## Arquitectura del proyecto 

### 01. Entidades (/model)
El sistema consta de 3 entidades principales y 2 clases enum que gestionan:

- Merchants: quienes crean los enlaces
- PaymentLinks: los enlaces de pago
- PaymentAttempts: intentos de pago relacionados a un enlace de pago específico.
  
 **Merchant**
  
```sh
@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    private UUID id;                // Identificador único
    private String name;            // Nombre del comercio
    private String email;           // Email único (para login)
    private String passwordHash;    // Contraseña encriptada
    private Instant createdAt;      // Fecha de registro
}
```
- Relaciones:

  - Un comercio puede tener múltiples PaymentLinks (relación 1:N)

- Restricciones:

  - Email único en el sistema

  - Campos obligatorios: name, email, passwordHash
 
**PaymentLink**

```sh
@Entity
@Table(name = "payment_links") 
public class PaymentLink {
    @Id
    private UUID id;                // ID único
    private String reference;       // Referencia legible (ej: PL-2023-0001)
    private Integer amountCents;    // Monto en centavos (>0)
    private String currency;        // Moneda (3 caracteres ISO)
    private String description;     
    private PaymentLinkStatus status; // CREATED/PAID/EXPIRED/CANCELLED
    private Instant expiresAt;      // Fecha de expiración
    private Instant paidAt;         // Fecha de pago (opcional)
    private String metadata;        // Datos adicionales (JSONB)
    private Instant createdAt;      // Fecha de creación
    
    @ManyToOne
    private Merchant merchant;      // Comercio asociado
}
```

- Índices:

  - expires_at para búsquedas de expiración
  
  - merchant_id + status para consultas filtradas

- Validaciones:

  - amountCents mínimo: 1

  - currency exactamente 3 caracteres
 
**PaymentAttempt**

```sh
@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {
    @Id
    private UUID id;
    
    @ManyToOne
    private PaymentLink paymentLink; // Enlace relacionado
    
    private PaymentAttemptStatus status; // SUCCESS/FAILED
    private String reason;           // Motivo de fallo (opcional)
    private String idempotencyKey;   // Clave para evitar duplicados
    private Instant createdAt;       // Fecha del intento
}
```
- Restricciones únicas:

  - No puede haber dos intentos con el mismo payment_link_id + idempotency_key

### 02. DTOs (/dto) 
Se crearon DTO de entrada y salida para proteger la estructura interna de las entidades, controlar qué datos se exponen en la API, validar las solicitudes entrantes, optimizar las respuestas según cada caso de uso.

Los DTO de entrada son los siguientes: 

**CreatePaymentLinkRequest**: para la creación de nuevos enlaces 
```sh
public class CreatePaymentLinkRequest {
    @Min(1)
    private Integer amountCents;     // Monto mínimo: 1 centavo
    
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;         // Formato ISO (3 caracteres)
    
    @NotBlank
    @Size(max = 255)
    private String description;      // Máximo 255 caracteres
    
    @Min(1)
    private Integer expiresInMinutes; // Mínimo 1 minuto
    
    private Map<String, Object> metadata; // Datos adicionales
}
```
**PayPaymentLinkRequest**: simulación de pagos 
```sh
public class PayPaymentLinkRequest {
    @NotBlank
    private String paymentToken; // Formato: "ok_" (éxito) o "fail_" (fallo)
}
```
Y los de salida: 
**PaymentLinkResponse (versión básica)**: campos esenciales para listados o respuestas simples
```sh
public class PaymentLinkResponse {
    private UUID id;
    private String reference;     // Ej: "PL-2023-0001"
    private Integer amountCents;
    private String currency;
    private PaymentLinkStatus status;
    private Instant expiresAt;
}
```
**PaymentLinkDetailsResponse (versión detallada)**: extiende la versión básica, incluye metadatos y fechas adicionales, muestra relación con merchant sin exponer datos sensibles
```sh
public class PaymentLinkDetailsResponse extends PaymentLinkResponse {
    private String description;
    private Instant paidAt;       // Null si no ha sido pagado
    private Instant createdAt;
    private Map<String, Object> metadata;
    private UUID merchantId;      // Solo el ID, no el objeto completo
}
```
**PaymentAttemptResponse**: dar respuestas de los endpoints de procesamiento de pagos
```sh
public class PaymentAttemptResponse {
    private UUID id;
    private String status;       // "SUCCESS" o "FAILED"
    private String reason;       // Null si fue exitoso
    private Instant createdAt;
    private UUID paymentLinkId;  // Referencia al enlace
}
```

### 03. Repositorios (/repositories)

### 04. Servicios (/service)

### 05. Controladores (/controller)

### 06. Manejo de errores (/exception)

### 07. Seguridad (/security)

