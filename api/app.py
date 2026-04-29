import os
import time
import oracledb
from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

DB_USER = os.environ.get("DB_USER", "VLS_ADMIN")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "Admin_VLS_2026")
DB_DSN = os.environ.get("DB_DSN", "oracle-db:1521/FREEPDB1")


def get_connection():
    return oracledb.connect(user=DB_USER, password=DB_PASSWORD, dsn=DB_DSN)


def rows_to_dicts(cursor):
    columns = [col[0].lower() for col in cursor.description]
    return [dict(zip(columns, row)) for row in cursor.fetchall()]


# --- Mapping Oracle values to Android enum names ---

STATUT_SATELLITE_MAP = {
    "Opérationnel": "OPERATIONNEL",
    "En veille": "EN_VEILLE",
    "Défaillant": "DEFAILLANT",
    "Désorbité": "DESORBITE",
}

FORMAT_CUBESAT_MAP = {
    "1U": "U1",
    "3U": "U3",
    "6U": "U6",
    "12U": "U12",
}

STATUT_FENETRE_MAP = {
    "Planifiée": "PLANIFIEE",
    "Réalisée": "REALISEE",
    "Annulée": "ANNULEE",
}

STATUT_STATION_MAP = {
    "Active": "ACTIVE",
    "Maintenance": "MAINTENANCE",
    "Inactive": "INACTIVE",
}


@app.route("/satellites", methods=["GET"])
def get_satellites():
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """SELECT s.id_satellite, s.nom_satellite, s.statut, s.format_cubesat,
                      s.id_orbite, s.date_lancement, s.masse,
                      s.duree_vie_prevue, s.capacite_batterie
                 FROM SATELLITE s ORDER BY s.id_satellite"""
        )
        rows = rows_to_dicts(cursor)
        result = []
        for r in rows:
            result.append({
                "idSatellite": r["id_satellite"],
                "nomSatellite": r["nom_satellite"],
                "statut": STATUT_SATELLITE_MAP.get(r["statut"], r["statut"]),
                "formatCubesat": FORMAT_CUBESAT_MAP.get(r["format_cubesat"], r["format_cubesat"]),
                "idOrbite": r["id_orbite"],
                "dateLancement": r["date_lancement"].strftime("%Y-%m-%d") if r["date_lancement"] else None,
                "masse": float(r["masse"]) if r["masse"] else None,
                "dureeViePrevue": r.get("duree_vie_prevue"),
                "capaciteBatterie": float(r["capacite_batterie"]) if r.get("capacite_batterie") else None,
            })
        return jsonify(result)
    finally:
        conn.close()


@app.route("/satellites/<satellite_id>", methods=["GET"])
def get_satellite(satellite_id):
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """SELECT s.id_satellite, s.nom_satellite, s.statut, s.format_cubesat,
                      s.id_orbite, s.date_lancement, s.masse,
                      s.duree_vie_prevue, s.capacite_batterie
                 FROM SATELLITE s WHERE s.id_satellite = :id""",
            {"id": satellite_id},
        )
        rows = rows_to_dicts(cursor)
        if not rows:
            return jsonify({"error": "Satellite not found"}), 404
        r = rows[0]
        return jsonify({
            "idSatellite": r["id_satellite"],
            "nomSatellite": r["nom_satellite"],
            "statut": STATUT_SATELLITE_MAP.get(r["statut"], r["statut"]),
            "formatCubesat": FORMAT_CUBESAT_MAP.get(r["format_cubesat"], r["format_cubesat"]),
            "idOrbite": r["id_orbite"],
            "dateLancement": r["date_lancement"].strftime("%Y-%m-%d") if r["date_lancement"] else None,
            "masse": float(r["masse"]) if r["masse"] else None,
            "dureeViePrevue": r.get("duree_vie_prevue"),
            "capaciteBatterie": float(r["capacite_batterie"]) if r.get("capacite_batterie") else None,
        })
    finally:
        conn.close()


@app.route("/satellites/<satellite_id>/instruments", methods=["GET"])
def get_instruments(satellite_id):
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """SELECT i.ref_instrument, i.type_instrument, i.modele,
                      i.resolution, i.consommation, i.masse,
                      e.etat_fonctionnement, e.date_integration
                 FROM EMBARQUEMENT e
                 JOIN INSTRUMENT i ON e.ref_instrument = i.ref_instrument
                WHERE e.id_satellite = :id
                ORDER BY i.ref_instrument""",
            {"id": satellite_id},
        )
        rows = rows_to_dicts(cursor)
        result = []
        for r in rows:
            result.append({
                "refInstrument": r["ref_instrument"],
                "typeInstrument": r["type_instrument"],
                "modele": r["modele"],
                "resolution": str(r["resolution"]) if r["resolution"] else None,
                "consommation": float(r["consommation"]) if r["consommation"] else None,
                "etatFonctionnement": r.get("etat_fonctionnement"),
                "dateIntegration": r["date_integration"].strftime("%Y-%m-%d") if r.get("date_integration") else None,
            })
        return jsonify(result)
    finally:
        conn.close()


@app.route("/fenetres", methods=["GET"])
def get_fenetres():
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """SELECT f.id_fenetre, f.datetime_debut, f.duree,
                      f.elevation_max, f.volume_donnees, f.statut,
                      f.id_satellite, f.code_station
                 FROM FENETRE_COM f ORDER BY f.datetime_debut"""
        )
        rows = rows_to_dicts(cursor)
        result = []
        for r in rows:
            dt = r["datetime_debut"]
            result.append({
                "idFenetre": r["id_fenetre"],
                "datetimeDebut": dt.strftime("%Y-%m-%dT%H:%M:%S") if dt else None,
                "duree": r["duree"],
                "elevationMax": float(r["elevation_max"]) if r["elevation_max"] else None,
                "statut": STATUT_FENETRE_MAP.get(r["statut"], r["statut"]),
                "idSatellite": r["id_satellite"],
                "codeStation": r["code_station"],
                "volumeDonnees": float(r["volume_donnees"]) if r["volume_donnees"] else None,
            })
        return jsonify(result)
    finally:
        conn.close()


@app.route("/stations", methods=["GET"])
def get_stations():
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """SELECT s.code_station, s.nom_station, s.latitude, s.longitude,
                      s.diametre_antenne, s.bande_frequence, s.debit_max, s.statut
                 FROM STATION_SOL s ORDER BY s.code_station"""
        )
        rows = rows_to_dicts(cursor)
        result = []
        for r in rows:
            result.append({
                "codeStation": r["code_station"],
                "nomStation": r["nom_station"],
                "latitude": float(r["latitude"]),
                "longitude": float(r["longitude"]),
                "diametreAntenne": float(r["diametre_antenne"]) if r["diametre_antenne"] else None,
                "bandeFrequence": r.get("bande_frequence"),
                "debitMax": float(r["debit_max"]) if r["debit_max"] else None,
                "statut": STATUT_STATION_MAP.get(r["statut"], r["statut"]),
            })
        return jsonify(result)
    finally:
        conn.close()


@app.route("/missions", methods=["GET"])
def get_missions():
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """SELECT m.id_mission, m.nom_mission, m.objectif,
                      m.zone_geo_cible, m.date_debut, m.date_fin, m.statut_mission
                 FROM MISSION m ORDER BY m.id_mission"""
        )
        rows = rows_to_dicts(cursor)
        result = []
        for r in rows:
            result.append({
                "idMission": r["id_mission"],
                "nomMission": r["nom_mission"],
                "objectif": r["objectif"],
                "zoneGeoCible": r.get("zone_geo_cible"),
                "dateDebut": r["date_debut"].strftime("%Y-%m-%d") if r["date_debut"] else None,
                "dateFin": r["date_fin"].strftime("%Y-%m-%d") if r.get("date_fin") else None,
                "statutMission": r["statut_mission"],
            })
        return jsonify(result)
    finally:
        conn.close()


@app.route("/satellites/<satellite_id>/missions", methods=["GET"])
def get_satellite_missions(satellite_id):
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """SELECT m.id_mission, m.nom_mission, m.objectif,
                      m.zone_geo_cible, m.date_debut, m.date_fin, m.statut_mission,
                      p.role_satellite
                 FROM PARTICIPATION p
                 JOIN MISSION m ON p.id_mission = m.id_mission
                WHERE p.id_satellite = :id
                ORDER BY m.date_debut DESC""",
            {"id": satellite_id},
        )
        rows = rows_to_dicts(cursor)
        result = []
        for r in rows:
            result.append({
                "idMission": r["id_mission"],
                "nomMission": r["nom_mission"],
                "objectif": r["objectif"],
                "zoneGeoCible": r.get("zone_geo_cible"),
                "dateDebut": r["date_debut"].strftime("%Y-%m-%d") if r["date_debut"] else None,
                "dateFin": r["date_fin"].strftime("%Y-%m-%d") if r.get("date_fin") else None,
                "statutMission": r["statut_mission"],
                "roleSatellite": r.get("role_satellite"),
            })
        return jsonify(result)
    finally:
        conn.close()


@app.route("/health", methods=["GET"])
def health():
    try:
        conn = get_connection()
        conn.close()
        return jsonify({"status": "ok", "database": "connected"})
    except Exception as e:
        return jsonify({"status": "error", "database": str(e)}), 503


def wait_for_db(max_retries=30, delay=5):
    for i in range(max_retries):
        try:
            conn = get_connection()
            conn.close()
            print("Database connection established.")
            return True
        except Exception as e:
            print(f"Waiting for database... ({i + 1}/{max_retries}) - {e}")
            time.sleep(delay)
    print("Failed to connect to database.")
    return False


if __name__ == "__main__":
    if wait_for_db():
        app.run(host="0.0.0.0", port=5000, debug=False)
    else:
        exit(1)
