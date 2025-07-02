Este es un proyecto Kotlin Multiplataforma dirigido a Android, iOS, Escritorio, Servidor.

* `/composeApp` es para el código que será compartido a través de sus aplicaciones Compose Multiplataforma.
  Contiene varias subcarpetas:
  - `commonMain` es para el código que es común para todos los objetivos.
  - Otras carpetas son para código Kotlin que se compilará sólo para la plataforma indicada en el nombre de la carpeta.
    Por ejemplo, si quieres usar CoreCrypto de Apple para la parte iOS de tu aplicación Kotlin,
 `iosMain` sería la carpeta adecuada para tales llamadas.

* `/iosApp` contiene las aplicaciones iOS. Incluso si estás compartiendo tu UI con Compose Multiplatform, 
 necesitas este punto de entrada para tu aplicación iOS. Aquí es también donde debes añadir el código SwiftUI para tu proyecto.

* `/server` es para la aplicación del servidor Ktor.

* `/shared` es para el código que será compartido entre todos los objetivos en el proyecto.
  La subcarpeta más importante es `commonMain`. Si lo prefiere, puede añadir código a las carpetas específicas de la plataforma aquí también.


