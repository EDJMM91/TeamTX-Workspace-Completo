import re

with open(r'd:\Team-Nacional-TX-Aragua\app\src\main\java\com\example\data\model\Models.kt', 'r', encoding='utf-8') as f:
    content = f.read()

def add_defaults(match):
    entity_str = match.group(0)
    # Add defaults based on types
    # Replace val/var name: Type, with val/var name: Type = Default,
    # except if it already has an equals sign.
    
    lines = entity_str.split('\n')
    new_lines = []
    
    for line in lines:
        # Match lines like "    val fullName: String," or "    val distanceKm: Int,"
        # But ignore lines that already have "=" 
        m = re.match(r'^(\s*(?:val|var)\s+\w+\s*:\s*)([a-zA-Z_0-9\?]+)(\s*,?)$', line)
        if m:
            prefix = m.group(1)
            type_name = m.group(2)
            suffix = m.group(3)
            
            # Decide default based on type_name
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
            elif type_name == 'PaymentCategory':
                default = ' = PaymentCategory.MEMBRESIA_MENSUAL'
            elif type_name == 'TransactionType':
                default = ' = TransactionType.INGRESO'
            elif type_name == 'PaymentMethod':
                default = ' = PaymentMethod.PAGO_MOVIL'
            elif type_name == 'ItemCategory':
                default = ' = ItemCategory.HERRAMIENTAS_RUTA'
            elif type_name == 'ItemCondition':
                default = ' = ItemCondition.EXCELENTE'
            elif type_name == 'EmergencyType':
                default = ' = EmergencyType.ACCIDENTADO_GASOLINA'
            elif type_name.endswith('?'):
                default = ' = null'
                
            new_lines.append(prefix + type_name + default + suffix)
        else:
            new_lines.append(line)
            
    return '\n'.join(new_lines)

# Apply to all data classes inside the file.
new_content = re.sub(r'data class \w+\s*\([^)]+\)', add_defaults, content, flags=re.MULTILINE)

with open(r'd:\Team-Nacional-TX-Aragua\app\src\main\java\com\example\data\model\Models.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)
