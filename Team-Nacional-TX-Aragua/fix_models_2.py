import re

with open(r'd:\Team-Nacional-TX-Aragua\app\src\main\java\com\example\data\model\Models.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
in_data_class = False

for line in lines:
    if line.strip().startswith('data class '):
        in_data_class = True
        
    if in_data_class:
        # Match lines like "    val fullName: String," or "    val distanceKm: Int,"
        # But ignore lines that already have "="
        m = re.match(r'^(\s*(?:val|var)\s+\w+\s*:\s*)([a-zA-Z_0-9\?]+)(\s*,?)$', line)
        if m:
            prefix = m.group(1)
            type_name = m.group(2)
            suffix = m.group(3)
            
            default = ''
            if type_name == 'String':
                default = ' = ""'
            elif type_name == 'Int':
                default = ' = 0'
            elif type_name == 'Long':
                default = ' = 0L'
            elif type_name == 'Double':
                default = ' = 0.0'
            elif type_name == 'Boolean':
                default = ' = false'
            elif type_name == 'MemberRole':
                default = ' = MemberRole.ASPIRANTE'
            elif type_name == 'NoticeCategory':
                default = ' = NoticeCategory.AVISO_OFICIAL'
            elif type_name == 'NoticePriority':
                default = ' = NoticePriority.NORMAL'
            elif type_name == 'RideStatus':
                default = ' = RideStatus.PROGRAMADA'
            elif type_name == 'PaymentCategory':
                default = ' = PaymentCategory.MEMBRESIA_MENSUAL'
            elif type_name == 'TransactionType':
                default = ' = TransactionType.INGRESO'
            elif type_name == 'PaymentMethod':
                default = ' = PaymentMethod.PAGO_MOVIL'
            elif type_name == 'PaymentStatus':
                default = ' = PaymentStatus.VERIFICADO'
            elif type_name == 'ItemCategory':
                default = ' = ItemCategory.HERRAMIENTAS_RUTA'
            elif type_name == 'ItemCondition':
                default = ' = ItemCondition.EXCELENTE'
            elif type_name == 'LoanStatus':
                default = ' = LoanStatus.ACTIVO'
            elif type_name == 'EmergencyType':
                default = ' = EmergencyType.ACCIDENTADO_GASOLINA'
            elif type_name == 'EmergencyStatus':
                default = ' = EmergencyStatus.ACTIVA'
            elif type_name == 'MessageType':
                default = ' = MessageType.TEXT'
            elif type_name.endswith('?'):
                default = ' = null'
                
            line = prefix + type_name + default + suffix + '\n'
            
        if line.strip() == ') {' or line.strip() == ')':
            # Could be the end of the data class parameters
            # Wait, this is a very basic heuristic.
            pass
            
    new_lines.append(line)

with open(r'd:\Team-Nacional-TX-Aragua\app\src\main\java\com\example\data\model\Models.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
