#!/usr/bin/env python3
"""Generate Excalidraw diagrams for the e-commerce backend.

Outputs .excalidraw files into ./erd/ (open with https://excalidraw.com or
the VSCode Excalidraw extension). Each file is a self-contained JSON document.

Run:  python3 scripts/gen-diagrams.py
"""
import json
import os
import time

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "erd")
NOW = int(time.time() * 1000)
_counter = [0]


def nid():
    _counter[0] += 1
    return "e%05d" % _counter[0]


def base(typ, x, y, w, h, **kw):
    return {
        "id": kw.get("id", nid()),
        "type": typ,
        "x": x, "y": y, "width": w, "height": h,
        "angle": 0,
        "strokeColor": kw.get("strokeColor", "#1e1e1e"),
        "backgroundColor": kw.get("backgroundColor", "transparent"),
        "fillStyle": kw.get("fillStyle", "solid"),
        "strokeWidth": kw.get("strokeWidth", 2),
        "strokeStyle": kw.get("strokeStyle", "solid"),
        "roughness": kw.get("roughness", 1),
        "opacity": kw.get("opacity", 100),
        "groupIds": kw.get("groupIds", []),
        "frameId": kw.get("frameId", None),
        "roundness": kw.get("roundness", None),
        "seed": _counter[0] * 7919 + 17,
        "version": 1,
        "versionNonce": _counter[0] * 131,
        "isDeleted": False,
        "boundElements": kw.get("boundElements", None),
        "updated": NOW,
        "link": None,
        "locked": False,
    }


def _est_text_w(text, size):
    lines = text.split("\n")
    longest = max((len(l) for l in lines), default=0)
    return longest * size * 0.62


def text(x, y, label, **kw):
    size = kw.get("fontSize", 20)
    lines = label.count("\n") + 1
    el = base("text", x, y, _est_text_w(label, size), size * 1.25 * lines, **kw)
    el["roundness"] = None
    el["text"] = label
    el["fontSize"] = size
    el["fontFamily"] = kw.get("fontFamily", 1)
    el["textAlign"] = kw.get("textAlign", "center")
    el["verticalAlign"] = kw.get("verticalAlign", "middle")
    el["containerId"] = kw.get("containerId", None)
    el["originalText"] = label
    el["lineHeight"] = 1.25
    el["autoResize"] = True
    return el


def rectangle(x, y, w, h, label=None, **kw):
    el = base("rectangle", x, y, w, h, **kw)
    el["roundness"] = {"type": 3}
    if label is not None:
        tid = nid()
        el["boundElements"] = [{"id": tid, "type": "text"}]
        t = text(x + w / 2, y + h / 2, label,
                 containerId=el["id"], id=tid,
                 fontSize=kw.get("fontSize", 18),
                 groupIds=el["groupIds"])
        return el, t
    return el


def ellipse(x, y, w, h, label=None, **kw):
    el = base("ellipse", x, y, w, h, **kw)
    el["roundness"] = {"type": 2}
    if label is not None:
        tid = nid()
        el["boundElements"] = [{"id": tid, "type": "text"}]
        t = text(x + w / 2, y + h / 2, label,
                 containerId=el["id"], id=tid,
                 fontSize=kw.get("fontSize", 18),
                 groupIds=el["groupIds"])
        return el, t
    return el


def line_element(x1, y1, x2, y2, **kw):
    ex, ey = min(x1, x2), min(y1, y2)
    el = base("line", ex, ey, abs(x2 - x1), abs(y2 - y1), **kw)
    el["roundness"] = {"type": 2}
    el["points"] = [[x1 - ex, y1 - ey], [x2 - ex, y2 - ey]]
    el["lastCommittedPoint"] = None
    el["startBinding"] = kw.get("startBinding", None)
    el["endBinding"] = kw.get("endBinding", None)
    el["startArrowhead"] = kw.get("startArrowhead", None)
    el["endArrowhead"] = kw.get("endArrowhead", None)
    return el


def arrow(x1, y1, x2, y2, start_id=None, end_id=None, **kw):
    ex, ey = min(x1, x2), min(y1, y2)
    el = base("arrow", ex, ey, abs(x2 - x1), abs(y2 - y1), **kw)
    el["roundness"] = {"type": 2}
    el["points"] = [[x1 - ex, y1 - ey], [x2 - ex, y2 - ey]]
    el["lastCommittedPoint"] = None
    el["startBinding"] = ({"elementId": start_id, "gap": kw.get("gap", 6),
                           "focus": 0.5} if start_id else None)
    el["endBinding"] = ({"elementId": end_id, "gap": kw.get("gap", 6),
                         "focus": 0.5} if end_id else None)
    el["startArrowhead"] = kw.get("startArrowhead", None)
    el["endArrowhead"] = kw.get("endArrowhead", "arrow")
    return el


def arrow_label(x, y, label, **kw):
    return text(x, y, label, fontSize=14, fontFamily=2, **kw)


def save(elements, name):
    os.makedirs(OUT_DIR, exist_ok=True)
    doc = {
        "type": "excalidraw",
        "version": 2,
        "source": "https://excalidraw.com",
        "elements": elements,
        "appState": {"gridSize": 20, "viewBackgroundColor": "#ffffff"},
        "files": {},
    }
    path = os.path.join(OUT_DIR, name)
    with open(path, "w") as f:
        json.dump(doc, f, indent=2)
    print("wrote %s (%d elements)" % (os.path.relpath(path), len(elements)))


# ----------------------------------------------------------------------------
# 1. LAYERED ARCHITECTURE
# ----------------------------------------------------------------------------
def architecture():
    E = []
    COLOR = "#dbeafe"
    SERVICE = "#dcfce7"
    DAO = "#fef3c7"
    ENT = "#fce7f3"
    SEC = "#fee2e2"
    EXT = "#e2e8f0"

    # Client
    cli, cli_t = rectangle(60, 40, 460, 70, "CLIENT\n(SPA / Mobile App)", fillStyle="solid", backgroundColor=EXT)
    E += [cli, cli_t]

    # Presentation layer container
    pres = rectangle(60, 190, 480, 190, None, fillStyle="solid", backgroundColor=COLOR)
    pres_t = text(300, 196, "PRESENTATION LAYER (Controllers)", fontSize=15, fontFamily=2)
    E += [pres, pres_t]
    subs = []
    for i, name in enumerate(["AuthController", "CheckoutController", "WebhookController", "Data REST endpoints"]):
        r, t = rectangle(85, 232 + (i % 2) * 70, 215, 58, name, fontSize=14)
        E += [r, t]
        subs.append(r)
    # arrow client -> presentation
    E.append(arrow(290, 110, 290, 190, start_id=cli["id"], end_id=pres["id"]))

    # Service layer
    svc = rectangle(60, 440, 480, 130, None, fillStyle="solid", backgroundColor=SERVICE)
    svc_t = text(300, 448, "SERVICE LAYER (Business Logic)", fontSize=15, fontFamily=2)
    E += [svc, svc_t]
    s1, s1_t = rectangle(85, 482, 210, 70, "AuthService\nregister / login / profile", fontSize=13)
    s2, s2_t = rectangle(315, 482, 210, 70, "CheckoutService\nplaceOrder / PaymentIntent", fontSize=13)
    E += [s1, s1_t, s2, s2_t]
    E.append(arrow(300, 380, 300, 440, start_id=pres["id"], end_id=svc["id"]))

    # DAO layer
    dao = rectangle(60, 630, 480, 120, None, fillStyle="solid", backgroundColor=DAO)
    dao_t = text(300, 638, "DATA ACCESS LAYER (Spring Data JPA)", fontSize=15, fontFamily=2)
    E += [dao, dao_t]
    d1, d1_t = rectangle(85, 672, 210, 60, "JpaRepository\nAppUser / Order / Product ...", fontSize=13)
    d2, d2_t = rectangle(315, 672, 210, 60, "Repositories\nfindByEmail / findByTrackingNumber", fontSize=13)
    E += [d1, d1_t, d2, d2_t]
    E.append(arrow(300, 570, 300, 630, start_id=svc["id"], end_id=dao["id"]))

    # Entity layer
    ent = rectangle(60, 800, 480, 70, None, fillStyle="solid", backgroundColor=ENT)
    ent_t = text(300, 818, "JPA ENTITIES\nAppUser, Customer, Order, Product, Address ...", fontSize=15, fontFamily=2)
    E += [ent, ent_t]
    E.append(arrow(300, 750, 300, 800, start_id=dao["id"], end_id=ent["id"]))

    # DB
    db = rectangle(60, 920, 480, 70, None, fillStyle="solid", backgroundColor=EXT)
    db_t = text(300, 948, "MySQL (docker db, port 3306)", fontSize=16)
    E += [db, db_t]
    E.append(arrow(300, 870, 300, 920, start_id=ent["id"], end_id=db["id"]))

    # Cross-cutting: Security (right)
    sec = rectangle(640, 190, 360, 200, None, fillStyle="solid", backgroundColor=SEC)
    sec_t = text(820, 198, "SECURITY (cross-cutting)", fontSize=15, fontFamily=2)
    E += [sec, sec_t]
    lines = ["SecurityFilterChain", "JwtService (HS256 local)", "JwtDecoder dual: HS256 + RS256 (Okta)", "BCryptPasswordEncoder"]
    yy = 240
    for ln in lines:
        E.append(text(820, yy, ln, fontSize=14))
        yy += 32
    E.append(arrow(540, 285, 640, 285, start_id=pres["id"], end_id=sec["id"], strokeColor="#b91c1c"))

    # DTO + Config (right)
    dto = rectangle(640, 430, 360, 70, None, fillStyle="solid", backgroundColor="#ede9fe")
    dto_t = text(820, 452, "DTOs\nAuthResponse, MeResponse, Purchase, PaymentInfo ...", fontSize=14, fontFamily=2)
    E += [dto, dto_t]
    cfg = rectangle(640, 540, 360, 120, None, fillStyle="solid", backgroundColor="#fefce8")
    cfg_t = text(820, 548, "CONFIG", fontSize=15, fontFamily=2)
    E += [cfg, cfg_t]
    for i, ln in enumerate(["SecurityConfiguration", "OpenApiConfig (Swagger)", "MyDataRestConfig", "DataSeeder (seed SQL)"]):
        E.append(text(820, 584 + i * 22, ln, fontSize=13))
    E.append(arrow(540, 465, 640, 465, start_id=svc["id"], end_id=dto["id"], strokeColor="#7c3aed"))

    # External services
    okta, okta_t = ellipse(720, 720, 240, 90, "OKTA / MOCK OIDC\n(issuer + JWKS, HTTPS)", fillStyle="solid", backgroundColor=EXT)
    E += [okta, okta_t]
    E.append(arrow(820, 540, 820, 720, start_id=sec["id"], end_id=okta["id"], strokeColor="#b91c1c"))
    E.append(text(846, 620, "issuer discovery\n+ JWKS RS256", fontSize=12))

    stripe, stripe_t = ellipse(720, 860, 240, 90, "STRIPE API\n(PaymentIntents, HTTPS)", fillStyle="solid", backgroundColor=EXT)
    E += [stripe, stripe_t]
    E.append(arrow(555, 525, 720, 900, start_id=svc["id"], end_id=stripe["id"], strokeColor="#0369a1"))
    E.append(text(590, 740, "create / retrieve /\nconfirm PaymentIntent", fontSize=12))

    save(E, "architecture-layers.excalidraw")


# ----------------------------------------------------------------------------
# 2. ER DIAGRAM
# ----------------------------------------------------------------------------
def er_diagram():
    E = []
    TABLE_COLOR = "#dbeafe"
    PK = "#fef3c7"

    def table(x, y, name, fields, fk_color=None):
        width = 250
        header = rectangle(x, y, width, 34, name, fontSize=16, fillStyle="solid", backgroundColor=TABLE_COLOR)
        body_h = len(fields) * 22 + 8
        body = rectangle(x, y + 34, width, body_h, "\n".join(fields),
                         fontSize=14, textAlign="left", backgroundColor="transparent")
        gid = "grp_" + name.replace(" ", "_")
        for el in (header[0], header[1], body[0], body[1]):
            el["groupIds"] = [gid]
        E.extend([header[0], header[1], body[0], body[1]])
        return x, y, width, 34 + body_h

    def rel(x1, y1, x2, y2, l1, l2, dashed=False, label_note=None):
        kw = {}
        if dashed:
            kw["strokeStyle"] = "dashed"
            kw["strokeColor"] = "#b45309"
        E.append(line_element(x1, y1, x2, y2, **kw))
        E.append(text(x1 + 4, y1 - 22, l1, fontSize=14, textAlign="left"))
        E.append(text(x2 + 4, y2 - 8, l2, fontSize=14, textAlign="left"))
        if label_note:
            E.append(text((x1 + x2) / 2, (y1 + y2) / 2 + 10, label_note, fontSize=12, fontFamily=2))

    # Tables
    c1 = table(80, 80, "country", ["id  (PK)", "code", "name"])
    s1 = table(80, 320, "state", ["id  (PK)", "name", "country_id  (FK)"])
    pc = table(420, 80, "product_category", ["id  (PK)", "category_name"])
    pr = table(420, 320, "product", ["id  (PK)", "sku", "name", "description", "unit_price",
                                     "image_url", "active", "units_in_stock",
                                     "date_created", "last_updated", "category_id  (FK)"])
    cu = table(760, 80, "customer", ["id  (PK)", "first_name", "last_name", "email"])
    od = table(760, 360, "orders", ["id  (PK)", "order_tracking_number", "total_quantity",
                                    "total_price", "status", "date_created", "last_updated",
                                    "customer_id  (FK)", "shipping_address_id  (FK)",
                                    "billing_address_id  (FK)"])
    oi = table(760, 700, "order_item", ["id  (PK)", "image_url", "unit_price", "quantity",
                                        "product_id", "order_id  (FK)"])
    ad = table(1120, 360, "address", ["id  (PK)", "street", "city", "state", "country", "zip_code"])
    au = table(1120, 80, "app_user", ["id  (PK)", "first_name", "last_name",
                                      "email  (unique, NOT NULL)", "password  (NOT NULL)"])

    # Relationships
    rel(c1[0] + c1[2], c1[1] + 40, s1[0] + c1[2], s1[1] + 20, "1", "N")
    rel(pc[0] + pc[2], pc[1] + 40, pr[0] + pc[2], pr[1] + 40, "1", "N")
    rel(cu[0] + cu[2], cu[1] + 40, od[0] + cu[2], od[1] + 40, "1", "N")
    rel(od[0] + od[2], od[1] + 60, oi[0] + od[2], oi[1] + 20, "1", "N")
    # orders -> address (two FKs)
    rel(od[0] + od[2], od[1] + 150, ad[0], ad[1] + 90, "N", "1", label_note="shipping / billing (2 FK)")
    # order_item.product_id -> product.id  (no JPA FK)
    rel(oi[0], oi[1] + 30, pr[0] + pr[2], pr[1] + 160, "N", "1", dashed=True,
        label_note="product_id plain Long, no JPA relation")

    save(E, "ERD.excalidraw")


# ----------------------------------------------------------------------------
# 3. DOCKER ARCHITECTURE
# ----------------------------------------------------------------------------
def docker_architecture():
    E = []
    HOST = "#f1f5f9"
    NET = "#e0e7ff"
    BORDER = "#334155"

    host = rectangle(60, 80, 980, 520, None, strokeColor=BORDER, fillStyle="solid", backgroundColor=HOST)
    E.append(host)
    E.append(text(550, 88, "Docker Host  (docker compose up -d --build)", fontSize=16, fontFamily=2))

    # db container
    db = rectangle(110, 160, 260, 150, None, strokeColor=BORDER, fillStyle="solid", backgroundColor=NET)
    db_t = text(240, 170, "db  (mysql:8.4)", fontSize=16)
    E += [db, db_t]
    for i, ln in enumerate(["port 3308 -> 3306", "MYSQL_DATABASE=ecommerce", "user / pass: ecommerce", "volume mysql-data"]):
        E.append(text(240, 205 + i * 24, ln, fontSize=13))
    E.append(text(110, 335, "mysql-data volume", fontSize=12))

    # oidc container
    oid = rectangle(430, 160, 260, 150, None, strokeColor=BORDER, fillStyle="solid", backgroundColor=NET)
    oid_t = text(560, 170, "oidc  (mock provider)", fontSize=16)
    E += [oid, oid_t]
    for i, ln in enumerate(["port 8085 -> 8085 (HTTPS)", "self-signed cert (dev)", "serves discovery + JWKS", "ISSUER_BASE=https://oidc:8085"]):
        E.append(text(560, 205 + i * 24, ln, fontSize=13))

    # app container
    app = rectangle(750, 160, 250, 150, None, strokeColor=BORDER, fillStyle="solid", backgroundColor=NET)
    app_t = text(875, 170, "app  (Spring Boot)", fontSize=16)
    E += [app, app_t]
    for i, ln in enumerate(["port 9898 -> 9898", "JAVA_TOOL_OPTIONS truststore", "combines cacerts + mock cert", "springboot-images-new.jar"]):
        E.append(text(875, 205 + i * 24, ln, fontSize=13))

    # internal arrows
    E.append(arrow(370, 235, 430, 235, start_id=db["id"], end_id=oid["id"],
                   strokeColor="#1d4ed8", endArrowhead=None))
    E.append(text(400, 218, "JDBC\njdbc:mysql://db:3306", fontSize=12, fontFamily=2, textAlign="left"))
    E.append(arrow(690, 235, 750, 235, start_id=oid["id"], end_id=app["id"],
                   strokeColor="#1d4ed8", endArrowhead=None))
    E.append(text(690, 218, "HTTPS issuer\ndiscovery + JWKS", fontSize=12, fontFamily=2, textAlign="left"))

    # env box
    env = rectangle(110, 380, 890, 90, None, strokeColor=BORDER, fillStyle="solid", backgroundColor="#fef9c3")
    env_t = text(555, 392, "host .env  (interpolated by compose)", fontSize=15, fontFamily=2)
    E += [env, env_t]
    for i, ln in enumerate(["STRIPE_SECRET_KEY · STRIPE_WEBHOOK_SECRET · JWT_SECRET · ALLOWED_ORIGINS · OKTA_ISSUER · DB_*"]):
        E.append(text(555, 425, ln, fontSize=13))
    E.append(arrow(555, 380, 555, 310, strokeColor="#ca8a04"))

    # browser
    br, br_t = ellipse(110, 520, 220, 90, "Browser / Client\n(localhost:9898)", fillStyle="solid", backgroundColor="#dcfce7")
    E += [br, br_t]
    E.append(arrow(220, 520, 875, 310, start_id=br["id"], end_id=app["id"], strokeColor="#15803d"))
    E.append(arrow_label(380, 470, "HTTP :9898  (REST / Swagger)"))

    # Stripe cloud
    st, st_t = ellipse(760, 520, 240, 90, "Stripe API\n(cloud)", fillStyle="solid", backgroundColor="#fee2e2")
    E += [st, st_t]
    E.append(arrow(880, 520, 875, 310, start_id=st["id"], end_id=app["id"], strokeColor="#b91c1c"))
    E.append(arrow_label(700, 470, "webhook  POST /api/webhook/stripe"))

    save(E, "docker-architecture.excalidraw")


if __name__ == "__main__":
    architecture()
    er_diagram()
    docker_architecture()
    print("done -> %s" % os.path.abspath(OUT_DIR))
