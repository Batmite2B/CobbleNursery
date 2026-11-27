# Configuración del servidor

Edita `config/cobblenursery.json` en el servidor o, si juegas solo, en tu instancia. Reinicia el servidor o vuelve a abrir el mundo. El mod no sobrescribe los archivos existentes: conserva tus precios y añade solo los campos que quieras cambiar. Si falta un campo, se usa el valor predeterminado. Si no es válido, también se registra un aviso.

Los archivos nuevos traen comentarios breves con `//` sobre cada sección. CobbleNursery acepta tanto estos comentarios como JSON sin comentarios.

- **Patios.** `habitatUnlockItem` acepta un ID de objeto registrado, como `minecraft:emerald`. El predeterminado es `cobblemon:relic_coin`. En `habitatUnlockCosts`, los patios II, III y IV cuestan 64, 128 y 192 respectivamente. El patio I ya está desbloqueado; un precio de 0 hace gratis el siguiente patio, sin saltarse el orden.
- **Crianza.** `breeding` permite cambiar el tiempo por huevo (300 s), el cupo por patio (8), si se permite Ditto (sí) y la probabilidad shiny (1 entre 4096).
- **Incubación.** `incubation` permite cambiar los segundos por cada 31 puntos de IV (180), el mínimo por huevo (60 s) y el cupo del nido (1 a 4, con 4 por defecto).
- **Bonos.** `bonuses` permite editar el porcentaje de refugio, luz y cuidador (15%, 15% y 30%), la luz mínima (10), el radio del cuidador (16 bloques), y qué especies y habilidades cuentan. Un porcentaje de 0 desactiva ese bono.
- **Tolvas.** `automation` permite desactivar por separado la salida de huevos de la terminal o la entrada al nido.

Reducir el cupo no borra huevos y cambiar precios no vuelve a bloquear patios. En multijugador, servidor y clientes deben usar la misma versión del mod; solo se edita el JSON del servidor. [Claves exactas, rangos y ejemplo](habitat-unlock-costs.md).

[JSON completo con los valores predeterminados](cobblenursery.example.json).
